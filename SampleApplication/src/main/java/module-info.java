module demo.fx.control.map {
  requires fx.control.map;

  requires javafx.controls;
  requires javafx.fxml;

  opens fxmapcontrol.sampleapplication to javafx.graphics,javafx.fxml;
}
