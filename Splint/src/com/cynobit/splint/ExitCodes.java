package com.cynobit.splint;

/**
 * (c) CynoBit 2019
 * Created by Francis Ilechukwu 1/22/2019.
 */
class ExitCodes {
    static final int BAD_ARGUMENTS = 2;
    static final int IO_EXCEPTION = 3;
    static final int INVALID_PACKAGE_NAME = 4;
    static final int DB_CONNECTION_ERROR = 5;
    static final int NO_APPLICATION_FOLDER = 6;
    static final int SPLINT_FOLDER_CREATE_FAILED = 7;
    static final int TECHNICAL_ISSUE = 8;
    static final int ERROR_INSTALL_PACKAGE = 9;
    static final int EXTRACTION_ERROR = 10;
    static final int NO_DESCRIPTOR = 11;
    static final int DESCRIPTOR_READ_ERROR = 12;
    static final int PATCHING_ERROR = 13;
    static final int NO_CORE_FOLDER = 14;
    static final int FOLDER_CREATION_FAILED = 15;
    static final int PACKAGE_EXISTS = 16;
    static final int PACKAGE_CREATE_ERROR = 17;
    static final int ERROR_PROCESSING_SPLINT_FILE = 18;
    static final int ERROR_CREATING_CI_PROJECT = 19;
    static final int UN_PATCH_FAILED = 20;
    static final int MITMA_ON_PACKAGE_DOWNLOAD = 21;
}
