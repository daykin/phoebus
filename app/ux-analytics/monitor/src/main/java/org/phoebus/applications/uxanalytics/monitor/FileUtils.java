package org.phoebus.applications.uxanalytics.monitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.phoebus.framework.preferences.PhoebusPreferenceService;

public class FileUtils {

    public static final String GIT_METADATA_DIR = ".git";
    public static final String SVN_METADATA_DIR = ".svn";
    public static final String HG_METADATA_DIR = ".hg";
    public static final String WEB_CONTENT_ROOT_SETTING_NAME = "PHOEBUS_WEB_CONTENT_ROOT";

    public static String getWebContentRoot(){
        //check if PHOEBUS_WEB_CONTENT_ROOT is set
        String webContentRoot;
        webContentRoot = System.getenv(WEB_CONTENT_ROOT_SETTING_NAME);
        if(webContentRoot != null){
            return webContentRoot;
        }
        //check if preference is set in phoebus.properties
        return PhoebusPreferenceService.userNodeForClass(FileUtils.class).get(WEB_CONTENT_ROOT_SETTING_NAME, null);
    }

    private static boolean isGitRepo(File fileObject){
        return isRepo(fileObject, GIT_METADATA_DIR);
    }

    private static boolean isSVNRepo(File fileObject){
        return isRepo(fileObject, SVN_METADATA_DIR);
    }

    private static boolean isMercurialRepo(File fileObject){
        return isRepo(fileObject, HG_METADATA_DIR);
    }

    private static boolean isRepo(File fileObject, String repoType){
       File file = new File(fileObject.getAbsolutePath() + File.separator + repoType);
         return file.exists() && file.isDirectory();
    }

    public static boolean isSourceRoot(File fileObject){
        return isGitRepo(fileObject) || isSVNRepo(fileObject) || isMercurialRepo(fileObject);
    }

    private static boolean isURL(final String path)
    {
        return path.startsWith("http://")  ||
                path.startsWith("https://") ||
                path.startsWith("ftp://");
    }

    //path is, at this point, assumed to be valid
    public static String findSourceRootOf(String path){
        //first check if it's a url
        if(isURL(path)){
            String webRootNoProtocol = getWebContentRoot();
            if(webRootNoProtocol == null){
                return null;
            }
            String pathNoProtocol = path.substring(path.indexOf("://") + 3);
            if(pathNoProtocol.startsWith(webRootNoProtocol)){
                return getWebContentRoot();
            }
            return null;
        }
        else{
            return findSourceRootOf(new File(path)).getAbsolutePath();
        }
    }

    public static File findSourceRootOf(File fileObject) {
        File directory = fileObject.getParentFile();
        while (directory != null) {
            if (isSourceRoot(directory)) {
                return directory.getParentFile();
            }
            directory = directory.getParentFile();
        }
        return null;
    }

    //re-implement here with Java MessageDigest, so we don't need to depend on elog
    private static String getFileSHA256(String path){
        InputStream fis;
        try {
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            fis = ModelResourceUtil.openResourceStream(path);
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                sha256Digest.update(byteArray, 0, bytesCount);
            }
            byte[] digestBytes = sha256Digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digestBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String getPathWithoutSourceRoot(String path){
        String pathNoProtocol = path;
        if(isURL(path)){
            pathNoProtocol = path.substring(path.indexOf("://") + 3);
            String webContentRoot = getWebContentRoot();
            String sourceRootNoProtocol = webContentRoot.substring(webContentRoot.indexOf("://") + 3);
            if(pathNoProtocol.startsWith(sourceRootNoProtocol)){
                return ModelResourceUtil.normalize(pathNoProtocol.substring(sourceRootNoProtocol.length()));
            }
            return null;
        }
        else{
            String sourceRoot = findSourceRootOf(path);
            return ModelResourceUtil.normalize(new File(path).getAbsolutePath().substring(sourceRoot.length()+1));
        }
    }

    public static String getSHA256Suffix(String path){
        try{
            return getFileSHA256(path).substring(0, 8);
        }
        catch(NullPointerException e){
            return null;
        }
    }


    public static String getAnalyticsPathFor(String path){
        String pathWithoutRoot = getPathWithoutSourceRoot(path);
        if(pathWithoutRoot == null){
            return null;
        }
        String first8OfSHA256 = getSHA256Suffix(path);
        if(first8OfSHA256 == null){
            return null;
        }
        return pathWithoutRoot + "_" + first8OfSHA256;
    }

}
