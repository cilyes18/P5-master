package com.example.prototype1.ENUMS;

public enum VARS {
    ;
    private static final String TAG = "VARS";
    public static final String SMS_URI_INBOX = "content://sms/inbox";
    public static final int REQUEST_CAMERA = 11,
            READ_PHONE_STATE = 22,
            DO_READ_EXTERNAL_STORAGE = 33,
            DO_WRITE_EXTERNAL_STORAGE = 44,
            FLAG_PHONE_VERIFY = 1,
            FLAG_PHONE_MAIN = 2,
            FLAG_PHONE_MESSAGE = 3,
            FLAG_PHONE_SETTINGS = 4;

    public static final long MAX_AUTH_TIME = 7 * 60 * 60 * 1000; // 7h in milliseconds or whatever amount of hours we will agree on.


//    public static FirebaseAuth yy(){
//        class ABCD{
//            final FirebaseAuth  a = FirebaseAuth.getInstance();
//
//            public synchronized FirebaseAuth getA() {
//                return a;
//            }
//        }
//        ABCD abcd = new ABCD();
//        return abcd.getA();
//    }


}


