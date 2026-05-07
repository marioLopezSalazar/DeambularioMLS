package com.iesaguadulce.deambulario.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.iesaguadulce.deambulario.R;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.model.pojos.Answer;
import com.iesaguadulce.deambulario.model.pojos.FaqItem;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.model.pojos.Student;

import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tools class containing path and file name constants, processing picture & video methods,
 * generating session results CSV file methods and loading FAQs from an XML file.
 *
 * @author Mario López Salazar.
 */
public abstract class FilesUtils {

    /**
     * Local path of compressed pictures.
     */
    public static final String PATH_COMPRESSED = "compressed_";

    /**
     * Session results file name prefix.
     */
    public static final String RESULTS_FILE_NAME = "Deambulario_";

    /**
     * Date format for session results file name suffix.
     */
    public static final String RESULTS_DATE_FORMAT = "yyyyMMdd_HHmm";


    /**
     * Compresses a picture from the gallery and saves the copy on the device cache.
     *
     * @param context     Context in where this method is called.
     * @param originalUri URI of original picture.
     * @return URI of compressed picture, or original URI if compression fails.
     */
    public static Uri compressImage(Context context, Uri originalUri) {

        String mimeType = context.getContentResolver().getType(originalUri);
        if (mimeType != null && mimeType.startsWith("video")) {
            return originalUri;
        }

        FileOutputStream outputStream = null;

        try {
            // Reading the original Bitmap, depending on the system version:
            Bitmap originalBitmap;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), originalUri);
                originalBitmap = ImageDecoder.decodeBitmap(source);
            } else {
                try (InputStream inputStream = context.getContentResolver().openInputStream(originalUri)) {
                    if (inputStream == null) return originalUri;
                    originalBitmap = BitmapFactory.decodeStream(inputStream);
                }
            }

            // Calculating the scale:
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            int maxWidth = 1200;
            int maxHeight = 1200;
            Bitmap bitmapToCompress = originalBitmap;

            // Only scaling when the picture is smaller than established limit:
            if (width > maxWidth || height > maxHeight) {
                float ratioBitmap = (float) width / (float) height;
                float ratioMax = (float) maxWidth / (float) maxHeight;

                int finalWidth = maxWidth;
                int finalHeight = maxHeight;
                if (ratioMax > ratioBitmap) {
                    finalWidth = (int) ((float) maxHeight * ratioBitmap);
                } else {
                    finalHeight = (int) ((float) maxWidth / ratioBitmap);
                }

                // Creating the scaled Bitmap:
                bitmapToCompress = Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true);
            }

            // Creating file on device cache:
            File tempFile = new File(context.getCacheDir(), PATH_COMPRESSED + System.currentTimeMillis() + ".jpg");
            if (!tempFile.createNewFile()) {
                return originalUri;
            }
            outputStream = new FileOutputStream(tempFile);

            // Compressing the scaled Bitmap:
            bitmapToCompress.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            return Uri.fromFile(tempFile);

        } catch (Exception e) {
            return originalUri;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException ignored) {
            }
        }
    }


    /**
     * Calculates the size of a video file.
     *
     * @param context The context in which this method is called.
     * @param uri     URI of the video file.
     * @return The size of the video, in Mb.
     */
    public static double getFileSizeInMB(Context context, Uri uri) {
        double sizeInMB = 0;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    long sizeInBytes = cursor.getLong(sizeIndex);
                    sizeInMB = (double) sizeInBytes / (1024 * 1024);
                }
            }
        } catch (Exception e) {
            sizeInMB = -1;
        }
        return sizeInMB;
    }


    /**
     * Creates a CSV file containing the students' answers on a route, and allows the teacher to store it.
     *
     * @param context  The context in which this method is called.
     * @param session  The session whose answers must include on the file.
     * @param students The list of students joined to the session.
     */
    public static void exportAndShareCsv(@NotNull Context context, Session session, List<Student> students) {

        // Generating CSV string:
        String csvContent = generateSessionCSV(context, session, students);

        // Formatting file name:
        SimpleDateFormat dateFormat = new SimpleDateFormat(RESULTS_DATE_FORMAT, Locale.getDefault());
        String formattedDate = dateFormat.format(session.getDate());
        String cleanTitle = session.getTitleSnapshot().replaceAll("[^a-zA-Z0-9.-]", "_");
        String fileName = RESULTS_FILE_NAME + cleanTitle + "_" + formattedDate + ".csv";

        // Creating file on device cache:
        File file = new File(context.getCacheDir(), fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(csvContent.getBytes());
            fos.flush();

            // Getting the cache file URI:
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);

            // Creating an Intent to launch the 'share file' intent:
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, fileName);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Creating the 'Choose' Android native sheet:
            Intent chooser = Intent.createChooser(intent, context.getString(R.string.download_answers));
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Launching the 'share file' intent:
            context.startActivity(chooser);

        } catch (IOException e) {
            Toast.makeText(context, R.string.error_network, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Generates a CSV format string containing activities' text and a table with students-answers.
     *
     * @param context  The context in which this method is called.
     * @param session  The session whose answers must be processed.
     * @param students List of students joined to the session.
     * @return String containing the results of the session, on CSV format.
     */
    public static String generateSessionCSV(@NotNull Context context, @NonNull Session session, List<Student> students) {
        StringBuilder csv = new StringBuilder();

        // Getting the list of the session activities:
        List<Activity> activities = session.getActivitiesSnapshot();

        // Document header:
        csv.append(context.getString(R.string.csv_route_label)).append(";")
                .append("\"").append(sanitizeForCsv(session.getTitleSnapshot())).append("\"\n");
        String date = "-";
        if (session.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            date = sdf.format(session.getDate());
        }
        csv.append(context.getString(R.string.csv_date_label)).append(";")
                .append("\"").append(date).append("\"\n");
        csv.append("\n");

        // Activities list block:
        csv.append(context.getString(R.string.TEXTS_OF_ACTIVITIES));
        if (activities != null && !activities.isEmpty()) {
            for (int i = 0; i < activities.size(); i++) {
                Activity act = activities.get(i);
                csv.append(context.getString(R.string.csv_activity)).append(i + 1).append(";");
                String activityText = act.getText() != null ? act.getText() : context.getString(R.string.csv_no_text);
                csv.append("\"").append(sanitizeForCsv(activityText)).append("\"\n");
            }
        } else {
            csv.append(context.getString(R.string.csv_no_activities));
        }

        // Separator:
        csv.append("\n\n");

        // Table header:
        csv.append(context.getString(R.string.csv_student));
        if (activities != null) {
            for (int i = 0; i < activities.size(); i++) {
                csv.append("Act. ").append(i + 1).append(";");
            }
        }
        csv.append("\n");

        // A row for each student:
        if (students != null) {
            for (Student student : students) {
                // Nick column:
                csv.append("\"").append(sanitizeForCsv(student.getNick())).append("\";");

                // Activities columns:
                if (activities != null) {
                    for (Activity act : activities) {
                        String responseText = "-"; // Default, when non response.
                        boolean found = false;
                        if (student.getAnswers() != null) {
                            for (int i = 0; i < student.getAnswers().size() && !found; i++) {
                                Answer ans = student.getAnswers().get(i);
                                if (ans.getActivityId().equals(act.getActivityId()) && ans.getGivenAnswer() != null) {
                                    responseText = ans.getGivenAnswer();
                                    found = true;
                                }
                            }
                        }
                        csv.append("\"").append(sanitizeForCsv(responseText)).append("\";");
                    }
                }
                csv.append("\n");
            }
        }

        return csv.toString();
    }


    /**
     * Cleans texts to avoid crashing CSV format. Replace " by ', and line breaks for spaces.
     *
     * @param text The text to clean.
     * @return Cleaned text.
     */
    @NonNull
    static String sanitizeForCsv(String text) {
        if (text == null) {
            return "";
        }

        // Replacing (") with ('):
        String sanitized = text.replace("\"", "'");
        // Replacing (\n) with ( ):
        return sanitized.replace("\n", " ");
    }


    /**
     * Parses an XML file from the res/raw directory containing a list of FAQs.
     * The XML must have a root node <faqs> and child nodes <faq> containing
     * <question> and <answer> tags.
     *
     * @param context       The application or activity context.
     * @param rawResourceId The ID of the raw resource.
     * @return A list of FaqItem objects parsed from the XML.
     */
    public static List<FaqItem> loadFaqsFromRaw(@NonNull Context context, int rawResourceId) {
        List<FaqItem> faqList = new ArrayList<>();
        FaqItem faq = null;
        String textContent = "";

        try {
            // Opening the raw resource as an InputStream:
            InputStream inputStream = context.getResources().openRawResource(rawResourceId);

            // Setting up the XmlPullParser:
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "UTF-8");

            int eventType = parser.getEventType();

            // Iterating through the XML document:
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tag = parser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (tag.equalsIgnoreCase("faq")) {
                            faq = new FaqItem("", "");
                        }
                        break;

                    case XmlPullParser.TEXT:
                        textContent = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tag.equalsIgnoreCase("faq") && faq != null) {
                            faqList.add(faq);
                        } else if (tag.equalsIgnoreCase("question") && faq != null) {
                            // Setting the question text:
                            faq.setQuestion(textContent.trim());
                        } else if (tag.equalsIgnoreCase("answer") && faq != null) {
                            // Setting the answer text:
                            faq.setAnswer(textContent.trim());
                        }
                        break;
                }

                // Moving to the next element:
                eventType = parser.next();
            }

            // Freeing resources:
            inputStream.close();

        } catch (Exception ignored) {
        }

        return faqList;
    }
}
