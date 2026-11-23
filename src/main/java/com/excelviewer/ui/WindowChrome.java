package com.excelviewer.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Native-like Windows caption buttons, window drag, maximize/restore, and edge resize
 * for an undecorated JavaFX stage.
 */
public final class WindowChrome {

    private static final int RESIZE_MARGIN = 7;
    private static final String MINIMIZE_GLYPH = "\uE921";
    private static final String MAXIMIZE_GLYPH = "\uE922";
    private static final String RESTORE_GLYPH = "\uE923";
    private static final String CLOSE_GLYPH = "\uE8BB";

    private final Stage stage;
    private final Region root;
    private final Button maximizeButton;
    private final boolean useFluentIcons;

    private boolean maximized;
    private double restoreX;
    private double restoreY;
    private double restoreWidth;
    private double restoreHeight;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean dragging;
    private ResizeDir resizeDir = ResizeDir.NONE;
    private double resizeStartX;
    private double resizeStartY;
    private double resizeStartWidth;
    private double resizeStartHeight;
    private double resizeStartStageX;
    private double resizeStartStageY;

    public WindowChrome(Stage stage, Region root, Region dragRegion, Button minimizeButton,
                        Button maximizeButton, Button closeButton) {
        this.stage = stage;
        this.root = root;
        this.maximizeButton = maximizeButton;
        this.useFluentIcons = Font.getFamilies().stream()
                .anyMatch(family -> family.equals("Segoe MDL2 Assets") || family.equals("Segoe Fluent Icons"));

        styleCaptionButton(minimizeButton, false);
        styleCaptionButton(maximizeButton, false);
        styleCaptionButton(closeButton, true);
        applyGlyphs();

        minimizeButton.setTooltip(new Tooltip("Minimize"));
        maximizeButton.setTooltip(new Tooltip("Maximize"));
        closeButton.setTooltip(new Tooltip("Close"));
        minimizeButton.setOnAction(event -> stage.setIconified(true));
        maximizeButton.setOnAction(event -> toggleMaximize());
        closeButton.setOnAction(event -> stage.close());

        installDrag(dragRegion);
        installResize();
        stage.iconifiedProperty().addListener((obs, wasIconified, iconified) -> {
            if (!iconified && maximized) {
                applyMaximizedBounds();
            }
        });
    }

    public static HBox captionButtons(Button minimize, Button maximize, Button close) {
        minimize.getStyleClass().addAll("caption-button", "caption-min");
        maximize.getStyleClass().addAll("caption-button", "caption-max");
        close.getStyleClass().addAll("caption-button", "caption-close");
        minimize.setFocusTraversable(false);
        maximize.setFocusTraversable(false);
        close.setFocusTraversable(false);

        HBox buttons = new HBox(minimize, maximize, close);
        buttons.getStyleClass().add("caption-buttons");
        buttons.setFillHeight(true);
        HBox.setHgrow(buttons, Priority.NEVER);
        return buttons;
    }

    public boolean isMaximized() {
        return maximized;
    }

    public void toggleMaximize() {
        if (maximized) {
            restore();
        } else {
            maximize();
        }
    }

    private void maximize() {
        restoreX = stage.getX();
        restoreY = stage.getY();
        restoreWidth = stage.getWidth();
        restoreHeight = stage.getHeight();
        maximized = true;
        applyMaximizedBounds();
        root.getStyleClass().add("maximized");
        applyGlyphs();
    }

    private void restore() {
        maximized = false;
        root.getStyleClass().remove("maximized");
        stage.setX(restoreX);
        stage.setY(restoreY);
        stage.setWidth(restoreWidth);
        stage.setHeight(restoreHeight);
        applyGlyphs();
    }

    private void applyMaximizedBounds() {
        Screen screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary());
        Rectangle2D bounds = screen.getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    private void applyGlyphs() {
        if (useFluentIcons) {
            maximizeButton.setText(maximized ? RESTORE_GLYPH : MAXIMIZE_GLYPH);
            maximizeButton.setStyle("-fx-font-family: 'Segoe MDL2 Assets', 'Segoe Fluent Icons'; -fx-font-size: 10px;");
        } else {
            maximizeButton.setText(maximized ? "❐" : "☐");
        }
        if (maximizeButton.getTooltip() != null) {
            maximizeButton.getTooltip().setText(maximized ? "Restore Down" : "Maximize");
        }
    }

    private void styleCaptionButton(Button button, boolean close) {
        if (useFluentIcons) {
            String glyph = close ? CLOSE_GLYPH : (button == maximizeButton ? MAXIMIZE_GLYPH : MINIMIZE_GLYPH);
            if (!close && button != maximizeButton) {
                glyph = MINIMIZE_GLYPH;
            }
            button.setText(glyph);
            button.setStyle("-fx-font-family: 'Segoe MDL2 Assets', 'Segoe Fluent Icons'; -fx-font-size: 10px;");
        } else if (close) {
            button.setText("✕");
        } else if (button == maximizeButton) {
            button.setText("☐");
        } else {
            button.setText("–");
        }
    }

    private void installDrag(Region dragRegion) {
        dragRegion.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || resizeDir != ResizeDir.NONE) {
                return;
            }
            if (isCaptionButton(event)) {
                return;
            }
            dragging = true;
            dragOffsetX = event.getScreenX() - stage.getX();
            dragOffsetY = event.getScreenY() - stage.getY();
        });

        dragRegion.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!dragging || event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (maximized) {
                double ratio = dragOffsetX / Math.max(stage.getWidth(), 1);
                restore();
                dragOffsetX = restoreWidth * ratio;
                dragOffsetY = Math.min(dragOffsetY, 48);
            }
            stage.setX(event.getScreenX() - dragOffsetX);
            stage.setY(event.getScreenY() - dragOffsetY);
        });

        dragRegion.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> dragging = false);

        dragRegion.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !isCaptionButton(event)) {
                toggleMaximize();
            }
        });
    }

    private void installResize() {
        Scene scene = stage.getScene();
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (maximized || dragging) {
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            resizeDir = directionAt(event.getSceneX(), event.getSceneY(), scene.getWidth(), scene.getHeight());
            scene.setCursor(cursorFor(resizeDir));
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (maximized || event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            resizeDir = directionAt(event.getSceneX(), event.getSceneY(), scene.getWidth(), scene.getHeight());
            if (resizeDir == ResizeDir.NONE) {
                return;
            }
            event.consume();
            resizeStartX = event.getScreenX();
            resizeStartY = event.getScreenY();
            resizeStartWidth = stage.getWidth();
            resizeStartHeight = stage.getHeight();
            resizeStartStageX = stage.getX();
            resizeStartStageY = stage.getY();
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (maximized || resizeDir == ResizeDir.NONE) {
                return;
            }
            double deltaX = event.getScreenX() - resizeStartX;
            double deltaY = event.getScreenY() - resizeStartY;
            double minW = stage.getMinWidth();
            double minH = stage.getMinHeight();
            switch (resizeDir) {
                case E -> stage.setWidth(clamp(resizeStartWidth + deltaX, minW, Double.MAX_VALUE));
                case S -> stage.setHeight(clamp(resizeStartHeight + deltaY, minH, Double.MAX_VALUE));
                case W -> resizeFromWest(deltaX, minW);
                case N -> resizeFromNorth(deltaY, minH);
                case SE -> {
                    stage.setWidth(clamp(resizeStartWidth + deltaX, minW, Double.MAX_VALUE));
                    stage.setHeight(clamp(resizeStartHeight + deltaY, minH, Double.MAX_VALUE));
                }
                case SW -> {
                    resizeFromWest(deltaX, minW);
                    stage.setHeight(clamp(resizeStartHeight + deltaY, minH, Double.MAX_VALUE));
                }
                case NE -> {
                    stage.setWidth(clamp(resizeStartWidth + deltaX, minW, Double.MAX_VALUE));
                    resizeFromNorth(deltaY, minH);
                }
                case NW -> {
                    resizeFromWest(deltaX, minW);
                    resizeFromNorth(deltaY, minH);
                }
                default -> {
                }
            }
            event.consume();
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (resizeDir != ResizeDir.NONE) {
                resizeDir = ResizeDir.NONE;
                scene.setCursor(Cursor.DEFAULT);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            if (resizeDir == ResizeDir.NONE && !dragging) {
                scene.setCursor(Cursor.DEFAULT);
            }
        });
    }

    private void resizeFromWest(double deltaX, double minW) {
        double newWidth = clamp(resizeStartWidth - deltaX, minW, Double.MAX_VALUE);
        double applied = resizeStartWidth - newWidth;
        stage.setX(resizeStartStageX + applied);
        stage.setWidth(newWidth);
    }

    private void resizeFromNorth(double deltaY, double minH) {
        double newHeight = clamp(resizeStartHeight - deltaY, minH, Double.MAX_VALUE);
        double applied = resizeStartHeight - newHeight;
        stage.setY(resizeStartStageY + applied);
        stage.setHeight(newHeight);
    }

    private static boolean isCaptionButton(MouseEvent event) {
        if (!(event.getTarget() instanceof Node node)) {
            return false;
        }
        while (node != null) {
            if (node.getStyleClass().contains("caption-button")
                    || node.getStyleClass().contains("caption-buttons")) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private static ResizeDir directionAt(double x, double y, double width, double height) {
        boolean left = x <= RESIZE_MARGIN;
        boolean right = x >= width - RESIZE_MARGIN;
        boolean top = y <= RESIZE_MARGIN;
        boolean bottom = y >= height - RESIZE_MARGIN;
        if (top && left) {
            return ResizeDir.NW;
        }
        if (top && right) {
            return ResizeDir.NE;
        }
        if (bottom && left) {
            return ResizeDir.SW;
        }
        if (bottom && right) {
            return ResizeDir.SE;
        }
        if (left) {
            return ResizeDir.W;
        }
        if (right) {
            return ResizeDir.E;
        }
        if (top) {
            return ResizeDir.N;
        }
        if (bottom) {
            return ResizeDir.S;
        }
        return ResizeDir.NONE;
    }

    private static Cursor cursorFor(ResizeDir dir) {
        return switch (dir) {
            case N -> Cursor.N_RESIZE;
            case S -> Cursor.S_RESIZE;
            case E -> Cursor.E_RESIZE;
            case W -> Cursor.W_RESIZE;
            case NE -> Cursor.NE_RESIZE;
            case NW -> Cursor.NW_RESIZE;
            case SE -> Cursor.SE_RESIZE;
            case SW -> Cursor.SW_RESIZE;
            case NONE -> Cursor.DEFAULT;
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum ResizeDir {
        NONE, N, S, E, W, NE, NW, SE, SW
    }
}
