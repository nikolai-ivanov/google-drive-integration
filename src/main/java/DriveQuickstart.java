import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class DriveQuickstart {
    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved credentials/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = DriveQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        Collection<String> scopes = new ArrayList<>();
        scopes.add(DriveScopes.DRIVE_APPDATA);
        scopes.add(DriveScopes.DRIVE_FILE);
        scopes.add(DriveScopes.DRIVE_METADATA);
        scopes.add(DriveScopes.DRIVE);

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    private static void downloadFile(Drive service, String fileId, String filename) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(filename);
        service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
    }

    public static List<File> getGoogleRootFolders(Drive service) throws IOException {
        return getGoogleSubFolders(service, null);
    }

    public static List<File> getGoogleFolder(Drive driveService, String folderName) throws IOException {
        String pageToken = null;

        List<File> list = new ArrayList<>();

        String query = " name contains '" + folderName + "' "
                + " and mimeType = 'application/vnd.google-apps.folder' ";

        do {
            FileList result = driveService.files().list().setQ(query).setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, createdTime)")
                    .setPageToken(pageToken).execute();
            for (File file : result.getFiles()) {
                list.add(file);
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return list;
    }

    public static List<File> getGoogleChildrenFiles(Drive driveService, String googleFolderIdParent) throws IOException {
        String pageToken = null;
        List<File> list = new ArrayList<>();

        String query = " mimeType != 'application/vnd.google-apps.folder' "
                    + " and '" + googleFolderIdParent + "' in parents";

        do {
            FileList result = driveService.files().list().setQ(query).setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, createdTime)")
                    .setPageToken(pageToken).execute();
            for (File file : result.getFiles()) {
                list.add(file);
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return list;
    }

    public static final List<File> getGoogleSubFolders(Drive driveService, String googleFolderIdParent) throws IOException {
        String pageToken = null;
        List<File> list = new ArrayList<File>();

        String query = null;
        if (googleFolderIdParent == null) {
            query = " mimeType = 'application/vnd.google-apps.folder' " //
                    + " and 'root' in parents";
        } else {
            query = " mimeType = 'application/vnd.google-apps.folder' " //
                    + " and '" + googleFolderIdParent + "' in parents";
        }

        do {
            FileList result = driveService.files().list().setQ(query).setSpaces("drive") //
                    // Fields will be assigned values: id, name, createdTime
                    .setFields("nextPageToken, files(id, name, createdTime)")//
                    .setPageToken(pageToken).execute();
            for (File file : result.getFiles()) {
                list.add(file);
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        //
        return list;
    }

    public static List<File> getGoogleFilesByName(Drive driveService, String fileNameLike) throws IOException {
        String pageToken = null;
        List<File> list = new ArrayList<File>();

        String query = " name contains '" + fileNameLike + "' "
                + " and mimeType != 'application/vnd.google-apps.folder' ";

        do {
            FileList result = driveService.files().list().setQ(query).setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, createdTime, mimeType)")
                    .setPageToken(pageToken).execute();
            for (File file : result.getFiles()) {
                list.add(file);
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return list;
    }

    /**
     * Download the file by filename
     * @param args
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static void main(String[] args) throws IOException, GeneralSecurityException {
        if(args.length != 1) {
            System.out.println(Arrays.toString(args));
            System.out.println("Please, provide the filename that you want to download");
            return;
        }

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME).build();


        List<File> rootGoogleFolders = getGoogleFolder(service, args[0]);
        if (rootGoogleFolders.size() == 0) {
            System.out.println("FolderId hasn't been found by name " + args[0]);
            return;
        }

        List<File> childrenFiles = getGoogleChildrenFiles(service, rootGoogleFolders.get(0).getId());
        for (File file : childrenFiles) {
            System.out.println(
                    " --- Name: " + file.getName() +
                    " --- Id: " + file.getId());
            downloadFile(service, file.getId(), file.getName());
        }

        System.out.println("Done!");
    }
}