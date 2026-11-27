package com.learnmate.app.core.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.learnmate.app.core.ai.AiCallback;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Copies user-selected PDFs into internal app storage and extracts text using PDFBox-Android.
 */
public final class PdfTextExtractor {

    private static final int MAX_EXTRACTED_CHARS = 100_000;

    private PdfTextExtractor() {}

    public static class PdfExtractResult {
        public final String suggestedTitle;
        public final String savedFilePath;
        public final int pageCount;
        public final String extractedText;

        public PdfExtractResult(String suggestedTitle, String savedFilePath, int pageCount, String extractedText) {
            this.suggestedTitle = suggestedTitle;
            this.savedFilePath = savedFilePath;
            this.pageCount = pageCount;
            this.extractedText = extractedText;
        }
    }

    public static void extractFromUri(Context context, Uri pdfUri, AiCallback<PdfExtractResult> callback) {
        Context appContext = context.getApplicationContext();
        AppExecutors.getInstance().diskIO().execute(() -> {
            File targetFile = null;
            try {
                String displayName = resolveDisplayName(appContext.getContentResolver(), pdfUri);
                String suggestedTitle = stripPdfExtension(displayName);

                File pdfDir = new File(appContext.getFilesDir(), "pdfs");
                if (!pdfDir.exists() && !pdfDir.mkdirs()) {
                    postError(callback, "Failed to create internal storage directory for PDFs.");
                    return;
                }

                targetFile = new File(pdfDir, UUID.randomUUID().toString() + ".pdf");
                try (InputStream in = appContext.getContentResolver().openInputStream(pdfUri);
                     FileOutputStream out = new FileOutputStream(targetFile)) {
                    if (in == null) {
                        postError(callback, "Unable to open selected PDF file.");
                        return;
                    }
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                int pageCount;
                String rawText;
                try (PDDocument document = PDDocument.load(targetFile)) {
                    if (document.isEncrypted()) {
                        deleteQuietly(targetFile);
                        postError(callback, "This PDF is password-protected and cannot be read.");
                        return;
                    }
                    pageCount = document.getNumberOfPages();
                    PDFTextStripper stripper = new PDFTextStripper();
                    rawText = stripper.getText(document);
                }

                String cleanedText = cleanText(rawText);
                if (cleanedText.isEmpty()) {
                    deleteQuietly(targetFile);
                    postError(callback, "No selectable text found in this PDF (it may be a scanned image).");
                    return;
                }

                PdfExtractResult result = new PdfExtractResult(
                        suggestedTitle,
                        targetFile.getAbsolutePath(),
                        pageCount,
                        cleanedText
                );
                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(result));
            } catch (Exception e) {
                deleteQuietly(targetFile);
                postError(callback, "Failed to process PDF: " + e.getMessage());
            }
        });
    }

    private static String resolveDisplayName(ContentResolver resolver, Uri uri) {
        try (Cursor c = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx != -1) {
                    String name = c.getString(idx);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Untitled Material.pdf";
    }

    private static String stripPdfExtension(String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) {
            return filename.substring(0, filename.length() - 4).trim();
        }
        return filename;
    }

    private static String cleanText(String raw) {
        if (raw == null) return "";
        String normalized = raw.replaceAll("\\r\\n?", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (normalized.length() > MAX_EXTRACTED_CHARS) {
            return normalized.substring(0, MAX_EXTRACTED_CHARS);
        }
        return normalized;
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static void postError(AiCallback<PdfExtractResult> callback, String message) {
        AppExecutors.getInstance().mainThread().execute(() -> callback.onError(message));
    }
}
