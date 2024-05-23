module com.empresa.hito2_programacion {
    requires javafx.controls;
    requires javafx.fxml;
    requires mongo.java.driver;


    opens com.empresa.hito2_programacion to javafx.fxml;
    exports com.empresa.hito2_programacion;
}