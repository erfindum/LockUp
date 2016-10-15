package com.smartfoxitsolutions.lockup.mediavault;

/**
 * Created by RAAJA on 08-10-2016.
 */

public class MediaVaultModel {


    public static String TABLE_NAME = "lockup_vault";

    public static String ID_COLUMN_NAME = "_id";
    public static String ORIGINAL_MEDIA_PATH = "original_media_path";
    public static String VAULT_MEDIA_PATH = "vault_media_path";
    public static String ORIGINAL_FILE_NAME="original_file_name";
    public static String VAULT_FILE_NAME = "file_name";
    public static String VAULT_BUCKET_NAME = "bucket_name";
    public static String VAULT_BUCKET_ID = "bucket_id";
    public static String FILE_EXTENSION = "file_extension";
    public static String MEDIA_TYPE = "media_type";
    public static String TIME_STAMP = "timestamp";
    public static String THUMBNAIL_PATH = "thumbnail_path";

    public static String getCreateSQLString(){
        return "CREATE TABLE "+TABLE_NAME+"("+ID_COLUMN_NAME+" INTEGER PRIMARY KEY, "+ORIGINAL_MEDIA_PATH+ " TEXT, "
                + VAULT_MEDIA_PATH+" TEXT, "+ORIGINAL_FILE_NAME+" TEXT, "+ VAULT_FILE_NAME +" TEXT, "
                +VAULT_BUCKET_NAME+" TEXT, "+VAULT_BUCKET_ID+" TEXT, "+FILE_EXTENSION+" TEXT, "
                + MEDIA_TYPE+" TEXT, "+TIME_STAMP+" TEXT, "+THUMBNAIL_PATH+" TEXT)";
    }

    public static String getUpgradeSQLString(){
        return "CREATE TABLE "+TABLE_NAME+"("+ID_COLUMN_NAME+" INTEGER PRIMARY KEY, "+ORIGINAL_MEDIA_PATH+ " TEXT, "
                + VAULT_MEDIA_PATH+" TEXT, "+ORIGINAL_FILE_NAME+" TEXT, "+ VAULT_FILE_NAME +" TEXT, "
                +VAULT_BUCKET_NAME+" TEXT, "+VAULT_BUCKET_ID+" TEXT, "+FILE_EXTENSION+" TEXT, "
                + MEDIA_TYPE+" TEXT, "+TIME_STAMP+" TEXT, "+THUMBNAIL_PATH+" TEXT)";
    }
}
