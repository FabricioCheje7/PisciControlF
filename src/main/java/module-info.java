module pe.edu.upeu.piscicontrolf {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires com.h2database;

    requires org.apache.pdfbox;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;


    opens pe.edu.upeu.piscicontrolf to javafx.fxml;
    opens pe.edu.upeu.piscicontrolf.controller to javafx.fxml;
    exports pe.edu.upeu.piscicontrolf;
}