package com.excelviewer;

import com.excelviewer.model.BomItem;
import com.excelviewer.model.DqItem;
import com.excelviewer.model.GenerationResult;
import com.excelviewer.model.QualificationDocument;
import com.excelviewer.model.QualificationRow;
import com.excelviewer.model.QualificationType;
import com.excelviewer.model.TagRecord;
import com.excelviewer.service.ExcelExporter;
import com.excelviewer.service.GeneratorService;
import com.excelviewer.service.MasterFiles;
import com.excelviewer.service.WordExporter;
import com.excelviewer.service.WordMasterExporter;
import com.excelviewer.ui.WindowChrome;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.StageStyle;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class TagGeneratorApp extends Application {

    private File masterFile;
    private File attextFile;
    private File bomMasterFile;
    private File dqMasterFile;
    private File lastDirectory;
    private GenerationResult result;

    private Label masterFileLabel;
    private Label attextFileLabel;
    private Label bomMasterFileLabel;
    private Label dqMasterFileLabel;
    private Label bannerLabel;
    private Button generateButton;
    private Button exportTagButton;
    private Button exportBomButton;
    private Button exportDqButton;
    private Button exportAllButton;
    private final EnumMap<QualificationType, QualificationView> qualificationViews = new EnumMap<>(QualificationType.class);

    private MetricCard totalTagsCard;
    private MetricCard matchedCard;
    private MetricCard missingCard;
    private MetricCard bomGroupsCard;
    private MetricCard bomQtyCard;
    private MetricCard dqTotalCard;
    private MetricCard dqReadyCard;
    private MetricCard dqMissingCard;

    private final ObservableList<TagRecord> tagItems = FXCollections.observableArrayList();
    private final ObservableList<BomItem> bomItems = FXCollections.observableArrayList();
    private final ObservableList<DqItem> dqItems = FXCollections.observableArrayList();
    private FilteredList<TagRecord> filteredTags;
    private FilteredList<BomItem> filteredBom;
    private FilteredList<DqItem> filteredDq;

    private TableView<TagRecord> tagTable;
    private TableView<BomItem> bomTable;
    private TableView<DqItem> dqTable;
    private VBox tagPanel;
    private VBox bomPanel;
    private VBox dqPanel;
    private Stage stage;
    private WindowChrome windowChrome;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("TAG LIST AND BOM AUTO-GENERATOR");
        stage.setResizable(true);
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.getIcons().add(createAppIcon());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        Button minimizeButton = new Button();
        Button maximizeButton = new Button();
        Button closeButton = new Button();
        HBox captionButtons = WindowChrome.captionButtons(minimizeButton, maximizeButton, closeButton);
        Region header = buildHeader(captionButtons);
        root.setTop(header);
        root.setCenter(buildWorkspace());
        root.setBottom(buildFooter());

        Scene scene = new Scene(root, 1440, 900);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);

        windowChrome = new WindowChrome(stage, root, header, minimizeButton, maximizeButton, closeButton);
        stage.show();
    }

    private Region buildHeader(HBox captionButtons) {
        Label captionTitle = new Label("VS Design Solutions");
        captionTitle.getStyleClass().add("caption-title");
        Region captionSpacer = new Region();
        HBox.setHgrow(captionSpacer, Priority.ALWAYS);
        HBox captionBar = new HBox(captionTitle, captionSpacer, captionButtons);
        captionBar.getStyleClass().add("caption-bar");
        captionBar.setAlignment(Pos.CENTER_LEFT);

        Label mark = new Label("TG");
        mark.getStyleClass().add("brand-mark");

        Label title = new Label("Tag List and BOM Auto-Generator");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Match P&ID ATTEXT to masters, then generate Tag List, BOM, DQ, IQ, OQ, FS and FAT");
        subtitle.getStyleClass().add("app-subtitle");

        VBox titles = new VBox(3, title, subtitle);
        titles.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(14, mark, titles);
        brand.getStyleClass().add("header-brand");
        brand.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(brand, Priority.ALWAYS);

        Label version = new Label("DESKTOP V2");
        version.getStyleClass().add("version-badge");

        HBox content = new HBox(16, brand, version);
        content.getStyleClass().add("header-content");
        content.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(captionBar, content);
        header.getStyleClass().add("app-header");
        return header;
    }

    private WritableImage createAppIcon() {
        Canvas canvas = new Canvas(64, 64);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.web("#0b2750"));
        graphics.fillRoundRect(0, 0, 64, 64, 16, 16);
        graphics.setFill(Color.web("#36c4eb"));
        graphics.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        graphics.fillText("TG", 14, 42);
        WritableImage icon = new WritableImage(64, 64);
        canvas.snapshot(null, icon);
        return icon;
    }

    private Region buildWorkspace() {
        VBox controls = buildControls();
        VBox tabs = buildTabs();
        tabs.setMinHeight(Region.USE_PREF_SIZE);

        VBox workspace = new VBox(16, controls, bannerLabel(), tabs);
        workspace.getStyleClass().add("workspace");
        workspace.setFillWidth(true);

        ScrollPane appScroll = new ScrollPane(workspace);
        appScroll.getStyleClass().add("app-scroll");
        appScroll.setFitToWidth(true);
        appScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        appScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        appScroll.setPannable(true);
        return appScroll;
    }

    private VBox buildControls() {
        masterFileLabel = fileNameLabel("No Master Tag List selected");
        attextFileLabel = fileNameLabel("No ATTEXT file selected");
        bomMasterFileLabel = fileNameLabel("No Master BOM selected");
        dqMasterFileLabel = fileNameLabel("No Master DQ selected");

        VBox masterField = fileField("Master Tag List", masterFileLabel,
                "Choose Tag List", this::chooseMasterFile);
        VBox attextField = fileField("ATTEXT File", attextFileLabel,
                "Choose ATTEXT", this::chooseAttextFile);
        VBox bomMasterField = fileField("Master BOM", bomMasterFileLabel,
                "Choose Master BOM", this::chooseBomMasterFile);
        VBox dqMasterField = fileField("Master DQ (Word or Excel)", dqMasterFileLabel,
                "Choose Master DQ", this::chooseDqMasterFile);

        generateButton = actionButton("Generate Data", "btn-primary");
        generateButton.setOnAction(event -> generate());

        exportTagButton = actionButton("Export Tag List", "btn-secondary");
        exportTagButton.setDisable(true);
        exportTagButton.setOnAction(event -> exportTagList());

        exportBomButton = actionButton("Export BOM", "btn-secondary");
        exportBomButton.setDisable(true);
        exportBomButton.setOnAction(event -> exportBom());

        exportDqButton = actionButton("Generate DQ Word", "btn-secondary");
        exportDqButton.setDisable(true);
        exportDqButton.setOnAction(event -> exportDqWord());

        exportAllButton = actionButton("Export All", "btn-primary");
        exportAllButton.setDisable(true);
        exportAllButton.setOnAction(event -> exportAll());

        for (QualificationType type : QualificationType.values()) {
            qualificationViews.put(type, new QualificationView(type));
        }

        HBox files = new HBox(16, masterField, attextField);
        files.getStyleClass().add("files-row");
        HBox.setHgrow(masterField, Priority.ALWAYS);
        HBox.setHgrow(attextField, Priority.ALWAYS);

        HBox documentMasters = new HBox(16, bomMasterField, dqMasterField);
        documentMasters.getStyleClass().add("files-row");
        HBox.setHgrow(bomMasterField, Priority.ALWAYS);
        HBox.setHgrow(dqMasterField, Priority.ALWAYS);

        HBox extraFiles = new HBox(12);
        extraFiles.getStyleClass().add("files-row");
        for (QualificationType type : QualificationType.values()) {
            QualificationView view = qualificationViews.get(type);
            VBox field = fileField("Master " + type.code(), view.fileLabel,
                    "Choose " + type.code(), () -> chooseQualificationFile(view));
            HBox.setHgrow(field, Priority.ALWAYS);
            extraFiles.getChildren().add(field);
        }

        FlowPane actions = new FlowPane(10, 10);
        actions.getStyleClass().add("actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getChildren().addAll(generateButton, exportTagButton, exportBomButton, exportDqButton);
        for (QualificationType type : QualificationType.values()) {
            actions.getChildren().add(qualificationViews.get(type).exportButton);
        }
        actions.getChildren().add(exportAllButton);

        Label noteIcon = new Label("i");
        noteIcon.getStyleClass().add("note-icon");
        Label note = new Label("Required: Master Tag List + ATTEXT. Optional: Master BOM (Excel), Master DQ / OQ / FS / IQ / FAT (Excel or Word .docx). Word V3 masters keep their protocol layout; only tags present on the generated Tag List are kept. If Master BOM or Master DQ is not selected, that document is built from the Master Tag List.");
        note.getStyleClass().add("note");
        note.setWrapText(true);
        HBox.setHgrow(note, Priority.ALWAYS);
        HBox noteRow = new HBox(10, noteIcon, note);
        noteRow.getStyleClass().add("note-row");
        noteRow.setAlignment(Pos.TOP_LEFT);

        VBox panel = new VBox(16, files, documentMasters, extraFiles, actions, noteRow);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private Label bannerLabel() {
        bannerLabel = new Label();
        bannerLabel.getStyleClass().add("banner");
        bannerLabel.setVisible(false);
        bannerLabel.setManaged(false);
        bannerLabel.setWrapText(true);
        return bannerLabel;
    }

    private VBox buildTabs() {
        ToggleGroup group = new ToggleGroup();
        ToggleButton tagTab = tabButton("Tag List", group, true);
        ToggleButton bomTab = tabButton("BOM Summary", group, false);
        ToggleButton dqTab = tabButton("Design Qualification", group, false);
        ToggleButton iqTab = tabButton("IQ", group, false);
        ToggleButton oqTab = tabButton("OQ", group, false);
        ToggleButton fsTab = tabButton("FS", group, false);
        ToggleButton fatTab = tabButton("FAT", group, false);

        HBox tabs = new HBox(6, tagTab, bomTab, dqTab, iqTab, oqTab, fsTab, fatTab);
        tabs.getStyleClass().add("tabs");
        tabs.setMaxWidth(Region.USE_PREF_SIZE);

        tagPanel = buildTagPanel();
        bomPanel = buildBomPanel();
        dqPanel = buildDqPanel();
        for (QualificationType type : QualificationType.values()) {
            qualificationViews.get(type).buildPanel();
        }

        List<VBox> allPanels = new ArrayList<>();
        allPanels.add(tagPanel);
        allPanels.add(bomPanel);
        allPanels.add(dqPanel);
        for (QualificationType type : QualificationType.values()) {
            allPanels.add(qualificationViews.get(type).panel);
        }
        for (int i = 1; i < allPanels.size(); i++) {
            allPanels.get(i).setVisible(false);
            allPanels.get(i).setManaged(false);
        }

        StackPane panels = new StackPane();
        panels.getChildren().addAll(allPanels);
        panels.setMinHeight(0);
        panels.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(panels, Priority.ALWAYS);
        for (VBox panel : allPanels) {
            panel.setMinHeight(0);
            panel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            StackPane.setAlignment(panel, Pos.TOP_LEFT);
        }

        Map<ToggleButton, VBox> tabMap = Map.of(
                tagTab, tagPanel,
                bomTab, bomPanel,
                dqTab, dqPanel,
                iqTab, qualificationViews.get(QualificationType.IQ).panel,
                oqTab, qualificationViews.get(QualificationType.OQ).panel,
                fsTab, qualificationViews.get(QualificationType.FS).panel,
                fatTab, qualificationViews.get(QualificationType.FAT).panel
        );

        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
                return;
            }
            VBox visible = tabMap.get(newToggle);
            if (visible != null) {
                showPanel(visible, allPanels.stream().filter(panel -> panel != visible).toArray(VBox[]::new));
            }
        });

        VBox section = new VBox(16, tabs, panels);
        section.setMinHeight(0);
        section.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(section, Priority.ALWAYS);
        return section;
    }

    private VBox buildTagPanel() {
        totalTagsCard = new MetricCard("Total Tags", "metric");
        matchedCard = new MetricCard("Matched", "metric metric-good");
        missingCard = new MetricCard("Missing", "metric metric-warn");

        HBox dashboard = metricRow(totalTagsCard, matchedCard, missingCard);

        TextField filter = filterField("Filter tag register...");
        filteredTags = new FilteredList<>(tagItems, unused -> true);
        filter.textProperty().addListener((obs, old, value) -> filteredTags.setPredicate(row ->
                matches(value, row.processFunction(), row.tagNumber(), row.letter(), row.generatedTag(),
                        row.nomenclature(), row.component(), row.make(), row.model(), row.status())));

        tagTable = new TableView<>(filteredTags);
        configureScrollableTable(tagTable);
        tagTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tagTable.setPlaceholder(new Label("Generate data to populate the tag register"));
        tagTable.setRowFactory(table -> statusRow());
        addColumn(tagTable, "Process Function", TagRecord::processFunction, 150);
        addColumn(tagTable, "Tag Number", TagRecord::tagNumber, 160);
        addColumn(tagTable, "Letter", TagRecord::letter, 90);
        addColumn(tagTable, "Line Key", TagRecord::lineKey, 90);
        addColumn(tagTable, "Machine Section", TagRecord::machineSection, 140);
        addColumn(tagTable, "Running Number", TagRecord::runningNumber, 130);
        addColumn(tagTable, "Generated Tag", TagRecord::generatedTag, 180);
        addColumn(tagTable, "Nomenclature", TagRecord::nomenclature, 220);
        addColumn(tagTable, "Component", TagRecord::component, 150);
        addColumn(tagTable, "Make", TagRecord::make, 130);
        addColumn(tagTable, "Model", TagRecord::model, 130);
        addStatusColumn(tagTable, "Status", TagRecord::status, 90);
        VBox.setVgrow(tagTable, Priority.ALWAYS);

        return tableSection(dashboard, "Generated Tag Register",
                "Matched records are marked green; new records are marked amber.", filter, tagTable);
    }

    private VBox buildBomPanel() {
        bomGroupsCard = new MetricCard("BOM Groups", "metric");
        bomQtyCard = new MetricCard("Total Quantity", "metric metric-good");

        HBox dashboard = metricRow(bomGroupsCard, bomQtyCard);

        TextField filter = filterField("Filter BOM summary...");
        filteredBom = new FilteredList<>(bomItems, unused -> true);
        filter.textProperty().addListener((obs, old, value) -> filteredBom.setPredicate(row ->
                matches(value, String.valueOf(row.serialNo()), row.component(), row.make(), row.model(),
                        String.valueOf(row.quantity()))));

        bomTable = new TableView<>(filteredBom);
        configureScrollableTable(bomTable);
        bomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        bomTable.setPlaceholder(new Label("Generate data to populate the BOM summary"));
        addColumn(bomTable, "Sr. No.", item -> String.valueOf(item.serialNo()), 90);
        addColumn(bomTable, "Component", BomItem::component, 220);
        addColumn(bomTable, "Make", BomItem::make, 200);
        addColumn(bomTable, "Model", BomItem::model, 200);
        TableColumn<BomItem, String> qty = addColumn(bomTable, "Quantity", item -> String.valueOf(item.quantity()), 120);
        qty.setStyle("-fx-alignment: CENTER-RIGHT;");
        VBox.setVgrow(bomTable, Priority.ALWAYS);

        return tableSection(dashboard, "Component, Make and Model Summary",
                "Quantity is grouped by Component, Make and Model. Values come from Master BOM when that file is selected; otherwise from the Master Tag List.",
                filter, bomTable);
    }

    private VBox buildDqPanel() {
        dqTotalCard = new MetricCard("DQ Tags", "metric");
        dqReadyCard = new MetricCard("Ready", "metric metric-good");
        dqMissingCard = new MetricCard("Incomplete", "metric metric-warn");

        HBox dashboard = metricRow(dqTotalCard, dqReadyCard, dqMissingCard);

        TextField filter = filterField("Filter design qualification...");
        filteredDq = new FilteredList<>(dqItems, unused -> true);
        filter.textProperty().addListener((obs, old, value) -> filteredDq.setPredicate(row ->
                matches(value, row.tag(), row.nomenclature(), row.instrumentType(), row.signalType(),
                        row.material(), row.make(), row.model())));

        dqTable = new TableView<>(filteredDq);
        configureScrollableTable(dqTable);
        dqTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        dqTable.setPlaceholder(new Label("Generate data to populate design qualification"));
        dqTable.setRowFactory(table -> completenessRow());
        addColumn(dqTable, "Tag No.", DqItem::tag, 170);
        addColumn(dqTable, "Nomenclature", DqItem::nomenclature, 220);
        addColumn(dqTable, "Instrument Type", DqItem::instrumentType, 160);
        addColumn(dqTable, "Signal Type", DqItem::signalType, 140);
        addColumn(dqTable, "Panel/Field Material", DqItem::material, 180);
        addColumn(dqTable, "Make", DqItem::make, 130);
        addColumn(dqTable, "Model", DqItem::model, 130);
        addColumn(dqTable, "Passed", item -> "□", 80);
        addColumn(dqTable, "Failed", item -> "□", 80);
        VBox.setVgrow(dqTable, Priority.ALWAYS);

        return tableSection(dashboard, "Autogenerated Design Qualification",
                "Unique matched P&ID tags. Master DQ can be Excel or the V3 Word protocol. Values come from that file when selected; otherwise from the Master Tag List.",
                filter, dqTable);
    }

    private VBox tableSection(HBox dashboard, String title, String caption, TextField filter, TableView<?> table) {
        Label heading = new Label(title);
        heading.getStyleClass().add("table-title");
        Label hint = new Label(caption);
        hint.getStyleClass().add("table-caption");
        VBox titles = new VBox(4, heading, hint);
        HBox.setHgrow(titles, Priority.ALWAYS);

        HBox head = new HBox(16, titles, filter);
        head.getStyleClass().add("table-card-head");
        head.setAlignment(Pos.CENTER_LEFT);
        filter.setMaxWidth(280);

        VBox tableCard = new VBox(head, table);
        tableCard.getStyleClass().add("table-card");
        tableCard.setMinHeight(0);
        tableCard.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        dashboard.setMinHeight(Region.USE_PREF_SIZE);
        dashboard.setMaxHeight(Region.USE_PREF_SIZE);

        VBox panel = new VBox(14, dashboard, tableCard);
        panel.setMinHeight(0);
        panel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private static void configureScrollableTable(TableView<?> table) {
        table.setMinHeight(360);
        table.setPrefHeight(440);
        table.setMaxHeight(440);
        table.setFixedCellSize(40);
        table.setPlaceholder(table.getPlaceholder() == null
                ? new Label("Generate data to populate this table")
                : table.getPlaceholder());
    }

    private Label buildFooter() {
        Label footer = new Label("Tag List and BOM Auto-Generator  •  Local desktop processing  •  Files remain on this device");
        footer.getStyleClass().add("footer");
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);
        return footer;
    }

    private void generate() {
        hideBanner();
        if (masterFile == null) {
            showBanner("Please select the Master Tag List.", true);
            return;
        }
        if (attextFile == null) {
            showBanner("Please select the ATTEXT file.", true);
            return;
        }

        generateButton.setDisable(true);
        generateButton.setText("Generating...");
        exportTagButton.setDisable(true);
        exportBomButton.setDisable(true);
        exportDqButton.setDisable(true);
        exportAllButton.setDisable(true);
        for (QualificationView view : qualificationViews.values()) {
            view.exportButton.setDisable(true);
        }

        File master = masterFile;
        File attext = attextFile;
        File bomMaster = bomMasterFile;
        File dqMaster = dqMasterFile;
        EnumMap<QualificationType, File> extraFiles = new EnumMap<>(QualificationType.class);
        for (QualificationView view : qualificationViews.values()) {
            if (view.file != null) {
                extraFiles.put(view.type, view.file);
            }
        }
        Task<GenerationResult> task = new Task<>() {
            @Override
            protected GenerationResult call() throws Exception {
                return new GeneratorService().generate(master, attext, extraFiles, bomMaster, dqMaster);
            }
        };
        task.setOnSucceeded(event -> {
            result = task.getValue();
            populate(result);
            generateButton.setDisable(false);
            generateButton.setText("Generate Data");
            String message = "Generated " + result.totalTags() + " tags  •  "
                    + result.matchedCount() + " matched  •  "
                    + result.missingCount() + " missing.";
            boolean warning = result.note() != null && !result.note().isBlank();
            if (warning) {
                message += "  •  " + result.note();
            }
            showBanner(message, warning);
        });
        task.setOnFailed(event -> {
            generateButton.setDisable(false);
            generateButton.setText("Generate Data");
            clearOutputs();
            Throwable error = task.getException();
            showBanner(error == null ? "Generation failed." : error.getMessage(), true);
        });
        Thread thread = new Thread(task, "tag-generate");
        thread.setDaemon(true);
        thread.start();
    }

    private void populate(GenerationResult data) {
        tagItems.setAll(data.tags());
        bomItems.setAll(data.bomItems());
        dqItems.setAll(data.dqItems());

        totalTagsCard.setValue(String.valueOf(data.totalTags()));
        matchedCard.setValue(String.valueOf(data.matchedCount()));
        missingCard.setValue(String.valueOf(data.missingCount()));
        bomGroupsCard.setValue(String.valueOf(data.bomItems().size()));
        bomQtyCard.setValue(String.valueOf(data.bomQuantity()));
        dqTotalCard.setValue(String.valueOf(data.dqItems().size()));
        dqReadyCard.setValue(String.valueOf(data.dqReadyCount()));
        dqMissingCard.setValue(String.valueOf(data.dqIncompleteCount()));

        exportTagButton.setDisable(data.tags().isEmpty());
        exportBomButton.setDisable(data.bomItems().isEmpty());
        exportDqButton.setDisable(data.dqItems().isEmpty());
        exportAllButton.setDisable(data.tags().isEmpty());
        for (QualificationView view : qualificationViews.values()) {
            view.bind(data.qualification(view.type));
        }
    }

    private void clearOutputs() {
        result = null;
        tagItems.clear();
        bomItems.clear();
        dqItems.clear();
        totalTagsCard.setValue("0");
        matchedCard.setValue("0");
        missingCard.setValue("0");
        bomGroupsCard.setValue("0");
        bomQtyCard.setValue("0");
        dqTotalCard.setValue("0");
        dqReadyCard.setValue("0");
        dqMissingCard.setValue("0");
        exportTagButton.setDisable(true);
        exportBomButton.setDisable(true);
        exportDqButton.setDisable(true);
        exportAllButton.setDisable(true);
        for (QualificationView view : qualificationViews.values()) {
            view.clear();
        }
    }

    private void exportTagList() {
        if (result == null || result.tags().isEmpty()) {
            showBanner("Generate the tag list before exporting.", true);
            return;
        }
        File destination = saveFile("Export Tag List", "Tag_List_Auto_Generated.xlsx",
                new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        if (destination == null) {
            return;
        }
        try {
            new ExcelExporter().exportTagList(result.tags(), destination);
            showBanner("Tag list exported to " + destination.getName(), false);
        } catch (Exception ex) {
            showError("Unable to export tag list", ex.getMessage());
        }
    }

    private void exportBom() {
        if (result == null || result.bomItems().isEmpty()) {
            showBanner("Generate the tag list before exporting the BOM.", true);
            return;
        }
        File destination = saveFile("Export BOM", "BOM_Summary_Auto_Generated.xlsx",
                new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        if (destination == null) {
            return;
        }
        try {
            new ExcelExporter().exportBom(result.bomItems(), destination);
            showBanner("BOM summary exported to " + destination.getName(), false);
        } catch (Exception ex) {
            showError("Unable to export BOM", ex.getMessage());
        }
    }

    private void exportDqWord() {
        if (result == null || result.dqItems().isEmpty()) {
            showBanner("Generate data before creating the DQ document.", true);
            return;
        }
        boolean fromWordMaster = MasterFiles.isDocx(dqMasterFile);
        String defaultName = fromWordMaster
                ? "Design_Qualification_Auto_Generated.docx"
                : "Design_Qualification_Auto_Generated.doc";
        FileChooser.ExtensionFilter filter = fromWordMaster
                ? new FileChooser.ExtensionFilter("Word Document (*.docx)", "*.docx")
                : new FileChooser.ExtensionFilter("Word Document (*.doc)", "*.doc");
        File destination = saveFile("Generate DQ Word", defaultName, filter);
        if (destination == null) {
            return;
        }
        try {
            if (fromWordMaster) {
                new WordMasterExporter().exportFiltered(dqMasterFile, result.tags(), destination);
            } else {
                new WordExporter().exportDq(result.dqItems(), destination);
            }
            showBanner("DQ Word document exported to " + destination.getName(), false);
        } catch (Exception ex) {
            showError("Unable to generate DQ Word", ex.getMessage());
        }
    }

    private void exportAll() {
        if (result == null || result.tags().isEmpty()) {
            showBanner("Generate data before exporting.", true);
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose folder for all generated files");
        applyInitialDirectory(chooser);
        File folder = chooser.showDialog(generateButton.getScene().getWindow());
        if (folder == null) {
            return;
        }
        lastDirectory = folder;

        List<String> written = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        ExcelExporter excel = new ExcelExporter();
        WordExporter word = new WordExporter();

        try {
            excel.exportTagList(result.tags(), new File(folder, "Tag_List_Auto_Generated.xlsx"));
            written.add("Tag List");
        } catch (Exception ex) {
            failed.add("Tag List: " + safeMessage(ex));
        }

        if (!result.bomItems().isEmpty()) {
            try {
                excel.exportBom(result.bomItems(), new File(folder, "BOM_Summary_Auto_Generated.xlsx"));
                written.add("BOM");
            } catch (Exception ex) {
                failed.add("BOM: " + safeMessage(ex));
            }
        }

        if (!result.dqItems().isEmpty()) {
            try {
                if (MasterFiles.isDocx(dqMasterFile)) {
                    new WordMasterExporter().exportFiltered(dqMasterFile, result.tags(),
                            new File(folder, "Design_Qualification_Auto_Generated.docx"));
                } else {
                    word.exportDq(result.dqItems(), new File(folder, "Design_Qualification_Auto_Generated.doc"));
                }
                written.add("DQ");
            } catch (Exception ex) {
                failed.add("DQ: " + safeMessage(ex));
            }
        }

        for (QualificationType type : QualificationType.values()) {
            QualificationDocument document = result.qualification(type);
            if (!document.masterProvided() || document.hasError() || document.rows().isEmpty()) {
                continue;
            }
            QualificationView view = qualificationViews.get(type);
            try {
                if (view != null && MasterFiles.isDocx(view.file)) {
                    new WordMasterExporter().exportFiltered(view.file, result.tags(),
                            new File(folder, MasterFiles.wordExportName(type.exportFileName())));
                } else {
                    word.exportQualification(document, new File(folder, type.exportFileName()));
                }
                written.add(type.code());
            } catch (Exception ex) {
                failed.add(type.code() + ": " + safeMessage(ex));
            }
        }

        if (written.isEmpty()) {
            showBanner(failed.isEmpty() ? "No files were available to export." : String.join("  •  ", failed), true);
            return;
        }
        String message = "Exported " + written.size() + " file(s) to " + folder.getName() + ": " + String.join(", ", written);
        if (!failed.isEmpty()) {
            message += "  •  Failed: " + String.join("  •  ", failed);
        }
        showBanner(message, !failed.isEmpty());
    }

    private static String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private void chooseBomMasterFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Master BOM");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls", "*.xlsm"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        applyInitialDirectory(chooser);
        File file = chooser.showOpenDialog(generateButton.getScene().getWindow());
        if (file != null) {
            bomMasterFile = file;
            lastDirectory = file.getParentFile();
            bomMasterFileLabel.setText(file.getName());
            markFileReady(bomMasterFileLabel);
        }
    }

    private void chooseDqMasterFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Master DQ");
        addDocumentMasterFilters(chooser);
        applyInitialDirectory(chooser);
        File file = chooser.showOpenDialog(generateButton.getScene().getWindow());
        if (file != null) {
            dqMasterFile = file;
            lastDirectory = file.getParentFile();
            dqMasterFileLabel.setText(file.getName());
            markFileReady(dqMasterFileLabel);
        }
    }

    private void chooseMasterFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Master Tag List");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls", "*.xlsm"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        applyInitialDirectory(chooser);
        File file = chooser.showOpenDialog(generateButton.getScene().getWindow());
        if (file != null) {
            masterFile = file;
            lastDirectory = file.getParentFile();
            masterFileLabel.setText(file.getName());
            markFileReady(masterFileLabel);
        }
    }

    private void chooseAttextFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose ATTEXT File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ATTEXT Files", "*.txt", "*.attext"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        applyInitialDirectory(chooser);
        File file = chooser.showOpenDialog(generateButton.getScene().getWindow());
        if (file != null) {
            attextFile = file;
            lastDirectory = file.getParentFile();
            attextFileLabel.setText(file.getName());
            markFileReady(attextFileLabel);
        }
    }

    private File saveFile(String title, String defaultName, FileChooser.ExtensionFilter filter) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(filter);
        chooser.setInitialFileName(defaultName);
        applyInitialDirectory(chooser);
        File file = chooser.showSaveDialog(generateButton.getScene().getWindow());
        if (file != null) {
            lastDirectory = file.getParentFile();
        }
        return file;
    }

    private void chooseQualificationFile(QualificationView view) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Master " + view.type.code());
        addDocumentMasterFilters(chooser);
        applyInitialDirectory(chooser);
        File file = chooser.showOpenDialog(generateButton.getScene().getWindow());
        if (file != null) {
            view.file = file;
            lastDirectory = file.getParentFile();
            view.fileLabel.setText(file.getName());
            markFileReady(view.fileLabel);
        }
    }

    private void exportQualification(QualificationView view) {
        if (result == null) {
            showBanner("Generate data before creating the " + view.type.code() + " document.", true);
            return;
        }
        QualificationDocument document = result.qualification(view.type);
        if (!document.masterProvided()) {
            showBanner("Select a Master " + view.type.code() + " file and generate data.", true);
            return;
        }
        if (document.hasError()) {
            showBanner(document.errorMessage(), true);
            return;
        }
        if (document.rows().isEmpty()) {
            showBanner("No " + view.type.code() + " records were generated from the selected master file.", true);
            return;
        }
        boolean fromWordMaster = MasterFiles.isDocx(view.file);
        String defaultName = fromWordMaster
                ? MasterFiles.wordExportName(view.type.exportFileName())
                : view.type.exportFileName();
        FileChooser.ExtensionFilter filter = fromWordMaster
                ? new FileChooser.ExtensionFilter("Word Document (*.docx)", "*.docx")
                : new FileChooser.ExtensionFilter("Word Document (*.doc)", "*.doc");
        File destination = saveFile("Generate " + view.type.code() + " Word", defaultName, filter);
        if (destination == null) {
            return;
        }
        try {
            if (fromWordMaster) {
                new WordMasterExporter().exportFiltered(view.file, result.tags(), destination);
            } else {
                new WordExporter().exportQualification(document, destination);
            }
            showBanner(view.type.code() + " Word document exported to " + destination.getName(), false);
        } catch (Exception ex) {
            showError("Unable to generate " + view.type.code() + " Word", ex.getMessage());
        }
    }

    private static void addDocumentMasterFilters(FileChooser chooser) {
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel or Word", "*.xlsx", "*.xls", "*.xlsm", "*.docx"),
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls", "*.xlsm"),
                new FileChooser.ExtensionFilter("Word Files", "*.docx"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
    }

    private void applyInitialDirectory(FileChooser chooser) {
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
    }

    private void applyInitialDirectory(DirectoryChooser chooser) {
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
    }

    private VBox fileField(String title, Label nameLabel, String buttonText, Runnable action) {
        Label heading = new Label(title);
        heading.getStyleClass().add("field-label");

        Button browse = new Button(buttonText);
        browse.getStyleClass().addAll("btn", "btn-ghost");
        browse.setOnAction(event -> action.run());

        HBox box = new HBox(10, nameLabel, browse);
        box.getStyleClass().add("file-box");
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        VBox field = new VBox(8, heading, box);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static Label fileNameLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("file-name-label");
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static Button actionButton(String text, String extraClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("btn", extraClass);
        button.setMinHeight(43);
        return button;
    }

    private static ToggleButton tabButton(String text, ToggleGroup group, boolean selected) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("tab");
        button.setToggleGroup(group);
        button.setSelected(selected);
        return button;
    }

    private static TextField filterField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("filter-field");
        field.setPrefWidth(260);
        return field;
    }

    private static void showPanel(VBox visible, VBox... hidden) {
        visible.setVisible(true);
        visible.setManaged(true);
        for (VBox panel : hidden) {
            panel.setVisible(false);
            panel.setManaged(false);
        }
    }

    private static boolean matches(String query, String... values) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private <S> TableColumn<S, String> addColumn(TableView<S> table, String title,
                                                Function<S, String> extractor, double width) {
        TableColumn<S, String> column = new TableColumn<>(title);
        column.setMinWidth(80);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new SimpleStringProperty(
                extractor.apply(cell.getValue()) == null ? "" : extractor.apply(cell.getValue())));
        table.getColumns().add(column);
        return column;
    }

    private void addStatusColumn(TableView<TagRecord> table, String title,
                                 Function<TagRecord, String> extractor, double width) {
        TableColumn<TagRecord, String> column = addColumn(table, title, extractor, width);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll("status-badge", "FOUND".equals(item) ? "status-found" : "status-new");
                setGraphic(badge);
                setText(null);
            }
        });
    }

    private TableRow<TagRecord> statusRow() {
        return new TableRow<>() {
            @Override
            protected void updateItem(TagRecord item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("found-row", "missing-row");
                if (!empty && item != null) {
                    getStyleClass().add(item.found() ? "found-row" : "missing-row");
                }
            }
        };
    }

    private TableRow<DqItem> completenessRow() {
        return new TableRow<>() {
            @Override
            protected void updateItem(DqItem item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("found-row", "missing-row");
                if (!empty && item != null) {
                    getStyleClass().add(item.complete() ? "found-row" : "missing-row");
                }
            }
        };
    }

    private void showBanner(String message, boolean error) {
        bannerLabel.setText(message);
        bannerLabel.getStyleClass().setAll("banner", error ? "banner-error" : "banner-success");
        bannerLabel.setVisible(true);
        bannerLabel.setManaged(true);
    }

    private void hideBanner() {
        bannerLabel.setText("");
        bannerLabel.setVisible(false);
        bannerLabel.setManaged(false);
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(header);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private static HBox metricRow(MetricCard... cards) {
        HBox dashboard = new HBox(14);
        dashboard.getStyleClass().add("dashboard");
        for (MetricCard card : cards) {
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            dashboard.getChildren().add(card);
        }
        return dashboard;
    }

    private static void markFileReady(Label nameLabel) {
        nameLabel.getStyleClass().add("file-name-ready");
        if (nameLabel.getParent() != null) {
            nameLabel.getParent().getStyleClass().add("file-box-ready");
        }
    }

    private final class QualificationView {
        private final QualificationType type;
        private File file;
        private final Label fileLabel;
        private final Button exportButton;
        private MetricCard totalCard;
        private MetricCard matchedCard;
        private MetricCard missingCard;
        private final ObservableList<QualificationRow> items = FXCollections.observableArrayList();
        private FilteredList<QualificationRow> filtered;
        private TableView<QualificationRow> table;
        private VBox panel;

        private QualificationView(QualificationType type) {
            this.type = type;
            this.fileLabel = fileNameLabel("No Master " + type.code() + " selected");
            this.exportButton = actionButton("Generate " + type.code() + " Word", "btn-secondary");
            this.exportButton.setDisable(true);
            this.exportButton.setOnAction(event -> exportQualification(this));
        }

        private void buildPanel() {
            totalCard = new MetricCard(type.code() + " Tags", "metric");
            matchedCard = new MetricCard("Matched", "metric metric-good");
            missingCard = new MetricCard("Missing in Master", "metric metric-warn");
            HBox dashboard = metricRow(totalCard, matchedCard, missingCard);

            TextField filter = filterField("Filter " + type.code() + "...");
            filtered = new FilteredList<>(items, unused -> true);
            filter.textProperty().addListener((obs, old, value) -> filtered.setPredicate(row -> {
                if (value == null || value.isBlank()) {
                    return true;
                }
                return row.searchText().contains(value.toLowerCase(Locale.ROOT));
            }));

            table = new TableView<>(filtered);
            configureScrollableTable(table);
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            table.setPlaceholder(new Label("Select Master " + type.code() + " Word or Excel and click Generate Data"));
            table.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(QualificationRow item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("found-row", "missing-row");
                    if (!empty && item != null) {
                        getStyleClass().add(item.matched() ? "found-row" : "missing-row");
                    }
                }
            });
            VBox.setVgrow(table, Priority.ALWAYS);
            panel = tableSection(dashboard, "Machine-specific " + type.title(),
                    "Compared against the generated Tag List. Accepts Excel (column 0 = Tag) or the V3 Word master.",
                    filter, table);
        }

        private void bind(QualificationDocument document) {
            items.setAll(document.rows());
            table.getColumns().clear();
            addColumn(table, "Tag No.", QualificationRow::tag, 180);
            for (int i = 0; i < document.headers().size(); i++) {
                final int index = i;
                addColumn(table, document.headers().get(i), row -> row.valueAt(index), 160);
            }
            addColumn(table, "Status", QualificationRow::status, 120);
            addColumn(table, "Passed", row -> "□", 80);
            addColumn(table, "Failed", row -> "□", 80);

            totalCard.setValue(String.valueOf(document.rows().size()));
            matchedCard.setValue(String.valueOf(document.matchedCount()));
            missingCard.setValue(String.valueOf(document.missingCount()));
            exportButton.setDisable(document.rows().isEmpty() || document.hasError());

            if (!document.masterProvided()) {
                table.setPlaceholder(new Label("Select Master " + type.code() + " Word or Excel and click Generate Data"));
            } else if (document.hasError()) {
                table.setPlaceholder(new Label(document.errorMessage()));
            } else if (document.rows().isEmpty()) {
                table.setPlaceholder(new Label("No " + type.code() + " rows matched the generated Tag List"));
            }
        }

        private void clear() {
            items.clear();
            table.getColumns().clear();
            totalCard.setValue("0");
            matchedCard.setValue("0");
            missingCard.setValue("0");
            exportButton.setDisable(true);
            table.setPlaceholder(new Label("Select Master " + type.code() + " Word or Excel and click Generate Data"));
        }
    }

    private static final class MetricCard extends VBox {
        private final Label valueLabel;

        private MetricCard(String title, String extraClass) {
            Label titleLabel = new Label(title.toUpperCase(Locale.ROOT));
            titleLabel.getStyleClass().add("metric-label");
            valueLabel = new Label("0");
            valueLabel.getStyleClass().add("metric-value");
            getStyleClass().addAll(extraClass.split(" "));
            getChildren().addAll(titleLabel, valueLabel);
            setMinWidth(160);
            setAlignment(Pos.CENTER_LEFT);
        }

        private void setValue(String value) {
            valueLabel.setText(value);
        }
    }
}
