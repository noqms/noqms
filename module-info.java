module com.noqms {
    requires gson;
    requires java.sql;
    
    opens com.noqms.framework to gson;
    
    exports com.noqms.framework to com.noqms;
    exports com.noqms;
}