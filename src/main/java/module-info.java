module plf.musicPlayer {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.fxml;
    requires rxcontrols;

    exports com.plf.player;
    opens com.plf.player to javafx.fxml;
}