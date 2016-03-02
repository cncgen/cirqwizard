package org.cirqwizard.fx;

import javafx.beans.binding.Bindings;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.cirqwizard.fx.controls.RealNumberTextFieldTableCell;
import org.cirqwizard.layers.Panel;
import org.cirqwizard.layers.PanelBoard;
import org.cirqwizard.logging.LoggerFactory;
import org.cirqwizard.settings.ApplicationConstants;
import org.cirqwizard.settings.SettingsFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PanelController extends ScreenController implements Initializable
{
    private static final KeyCodeCombination KEY_CODE_ZOOM_IN = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCodeCombination KEY_CODE_ZOOM_OUT = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN);

    @FXML private ComboBox<PCBSize> sizeComboBox;
    @FXML private ScrollPane scrollPane;
    @FXML private PanelPane panelPane;

    @FXML TableView<PanelBoard> boardsTable;
    @FXML TableColumn<PanelBoard, String> boardFileColumn;
    @FXML TableColumn<PanelBoard, Integer> boardXColumn;
    @FXML TableColumn<PanelBoard, Integer> boardYColumn;
    @FXML TableColumn<PanelBoard, Boolean> boardOutlineColumn;

    @FXML private VBox errorBox;
    private CheckBox ignoreErrorCheckBox;
    @FXML private Button continueButton;

    private boolean resetCacheOnChange = true;

    @Override
    protected String getFxmlName()
    {
        return "Panel.fxml";
    }

    @Override
    protected String getName()
    {
        return "Panel";
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        scrollPane.viewportBoundsProperty().addListener((v, oldV, newV) -> zoomToFit(false));
        sizeComboBox.getItems().addAll(PCBSize.values());
        sizeComboBox.getSelectionModel().selectedItemProperty().addListener((v, oldV, newV) ->
        {
            panelPane.getPanel().setSize(newV);
            if (resetCacheOnChange)
                panelPane.getPanel().resetCacheTimestamps();
            savePanel();
            zoomToFit(true);
        });

        boardsTable.getSelectionModel().selectedItemProperty().addListener((v, oldV, newV) -> panelPane.selectBoard(newV));
        boardFileColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));
        boardFileColumn.setEditable(false);
        boardFileColumn.setCellFactory(p -> new TextFieldTableCell<PanelBoard, String>()
        {
            @Override
            public void updateItem(String item, boolean empty)
            {
                super.updateItem(item != null ? item.substring(item.lastIndexOf(File.separatorChar) + 1, item.length()) : null, empty);
                setTooltip(item == null ? null : new Tooltip(item));
            }
        });
        boardXColumn.setCellValueFactory(new PropertyValueFactory<>("x"));
        boardXColumn.setCellFactory(p -> new RealNumberTextFieldTableCell<>());
        boardXColumn.setOnEditCommit(event ->
        {
            event.getRowValue().setX(event.getNewValue());
            panelPane.getPanel().resetCacheTimestamps();
            validateBoards();
            savePanel();
            panelPane.render();
        });
        boardYColumn.setCellValueFactory(new PropertyValueFactory<>("y"));
        boardYColumn.setCellFactory(p -> new RealNumberTextFieldTableCell<>());
        boardYColumn.setOnEditCommit(event ->
        {
            event.getRowValue().setY(event.getNewValue());
            panelPane.getPanel().resetCacheTimestamps();
            validateBoards();
            savePanel();
            panelPane.render();
        });

        panelPane.setBoardDragListener(() ->
        {
            panelPane.getPanel().resetCacheTimestamps();
            panelPane.getPanel().save(getMainApplication().getContext().getPanelFile());
            validateBoards();
            refreshTable();
        });

        ignoreErrorCheckBox = new CheckBox("Ignore the errors. I know what I am doing");
        continueButton.disableProperty().bind(Bindings.and(Bindings.isNotEmpty(errorBox.getChildren()),
                Bindings.not(ignoreErrorCheckBox.selectedProperty())));
    }

    @Override
    public void refresh()
    {
        resetCacheOnChange = false;
        panelPane.setPanel(getMainApplication().getContext().getPanel());
        sizeComboBox.getSelectionModel().select(panelPane.getPanel().getSize());
        resetCacheOnChange = true;
        refreshTable();
        zoomToFit(true);
        getMainApplication().getContext().setG54X(SettingsFactory.getMachineSettings().getReferencePinX().getValue() -
                ApplicationConstants.getRegistrationPinsInset());
        getMainApplication().getContext().setG54Y(SettingsFactory.getMachineSettings().getReferencePinY().getValue() -
                ApplicationConstants.getRegistrationPinsInset());
        validateBoards();
    }

    @Override
    public EventHandler<? super KeyEvent> getShortcutHandler()
    {
        return event -> {

            if (event.isConsumed())
                return;
            if (KEY_CODE_ZOOM_IN.match(event))
            {
                zoomIn();
                event.consume();
            }
            else if (KEY_CODE_ZOOM_OUT.match(event))
            {
                zoomOut();
                event.consume();
            }
        };
    }

    private void refreshTable()
    {
        boardsTable.getItems().clear();
        boardsTable.getItems().addAll(panelPane.getPanel().getBoards());
    }

    public void rotateCw()
    {
        if (panelPane.getSelectedBoard() != null)
        {
            panelPane.getSelectedBoard().rotate(true);
            panelPane.getPanel().save(getMainApplication().getContext().getPanelFile());
            panelPane.render();
        }
    }

    public void rotateCcw()
    {
        if (panelPane.getSelectedBoard() != null)
        {
            panelPane.getSelectedBoard().rotate(false);
            panelPane.getPanel().save(getMainApplication().getContext().getPanelFile());
            panelPane.render();
        }
    }

    public void zoomIn()
    {
        panelPane.zoomIn();
    }

    public void zoomOut()
    {
        panelPane.zoomOut();
    }

    public void zoomToFit(boolean force)
    {
        panelPane.zoomToFit(scrollPane.getViewportBounds().getWidth(), scrollPane.getViewportBounds().getHeight(), force);
    }

    public void addBoard()
    {
        try
        {
            FileChooser chooser = new FileChooser();
            FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Gerber files", "*.sol", "*.cmp");
            chooser.getExtensionFilters().add(filter);
            File file = chooser.showOpenDialog(null);
            String filename = file.getAbsolutePath();
            String commonName = filename.substring(0, filename.lastIndexOf('.'));

            PanelBoard board = new PanelBoard(commonName, 0, 0);
            board.loadBoard();
            PCBSize panelSize = sizeComboBox.getSelectionModel().getSelectedItem();
            board.setX((panelSize.getWidth() - board.getBoard().getWidth()) / 2);
            board.setY((panelSize.getHeight() - board.getBoard().getHeight()) / 2);
            panelPane.getPanel().addBoard(board);
            panelPane.getPanel().resetCacheTimestamps();
            savePanel();
            panelPane.render();
            refreshTable();
            validateBoards();
        }
        catch (IOException e)
        {
            LoggerFactory.logException("Error adding board", e);
        }
    }

    private void savePanel()
    {
        Panel panel = getMainApplication().getContext().getPanel();
        panel.save(getMainApplication().getContext().getPanelFile());
    }

    private void validateBoards()
    {
        errorBox.getChildren().clear();
        Panel panel = panelPane.getPanel();
        panel.getBoards().stream().forEach(b ->
        {
            if (b.getX() < 0 || b.getY() < 0 || b.getX() + b.getBoard().getWidth() > panel.getSize().getWidth() ||
                    b.getY() + b.getBoard().getHeight() > panel.getSize().getHeight())
            {
                String filename = b.getFilename();
                filename = filename.substring(filename.lastIndexOf(File.separatorChar) + 1, filename.length());
                Label label = new Label("Board " + filename + " does not fit in the panel");
                label.setWrapText(true);
                label.getStyleClass().add("error-box");
                label.setPrefWidth(1000);
                errorBox.getChildren().add(label);
            }
        });
        if (!errorBox.getChildren().isEmpty())
        {
            errorBox.getChildren().add(ignoreErrorCheckBox);
        }
        errorBox.setVisible(!errorBox.getChildren().isEmpty());
        errorBox.setManaged(errorBox.isVisible());
        ignoreErrorCheckBox.setSelected(false);
    }

}