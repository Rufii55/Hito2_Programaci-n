package com.empresa.hito2_programacion;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;

import org.bson.Document;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;

import java.util.Optional;

public class HelloController {
    @FXML
    private ListView<String> datos;
    @FXML
    private TextField nombre, cantidad, searchField;
    @FXML
    private Button enviar, cargarProductos;

    private ObservableList<String> itemList;
    private FilteredList<String> filteredData;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public HelloController() {
        itemList = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(itemList, p -> true); // Inicializa la lista filtrada con un predicado que permite todo
        String connectionString = "mongodb+srv://hito:1234@elrufis.rsl75kf.mongodb.net/";
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase("hito");
        collection = database.getCollection("clientes");
    }

    @FXML
    public void initialize() {
        datos.setItems(filteredData);  // Configura la ListView para usar datos filtrados
        setupContextMenu();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(product -> {
                // Si el texto de búsqueda está vacío, muestra todos los productos.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Compara el nombre del producto en la lista con el texto de búsqueda.
                String lowerCaseFilter = newValue.toLowerCase();

                if (product.toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filtra coincide.
                }
                return false; // No coincide.
            });
        });
    }

    private void setupContextMenu() {
        datos.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>();
            ContextMenu contextMenu = new ContextMenu();

            MenuItem editItem = new MenuItem("Editar");
            editItem.setOnAction(event -> {
                String item = cell.getItem();
                if (item != null) {
                    String[] parts = item.split(": ");
                    String currentName = parts[0];
                    String currentQuantity = parts[1];

                    TextInputDialog dialog = new TextInputDialog(currentName);
                    dialog.setTitle("Editar Artículo");
                    dialog.setHeaderText("Editar nombre del artículo");
                    dialog.setContentText("Nuevo nombre:");
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(newName -> {
                        TextInputDialog quantityDialog = new TextInputDialog(currentQuantity);
                        quantityDialog.setTitle("Editar Artículo");
                        quantityDialog.setHeaderText("Editar cantidad del artículo");
                        quantityDialog.setContentText("Nueva cantidad:");
                        Optional<String> quantityResult = quantityDialog.showAndWait();
                        quantityResult.ifPresent(newQuantity -> {
                            Document filter = new Document("name", currentName).append("quantity", currentQuantity);
                            Document updatedDoc = new Document("name", newName).append("quantity", newQuantity);
                            collection.replaceOne(filter, updatedDoc);
                            itemList.set(cell.getIndex(), newName + ": " + newQuantity);
                        });
                    });
                }
            });

            MenuItem deleteItem = new MenuItem("Eliminar");
            deleteItem.setOnAction(event -> {
                String item = cell.getItem();
                if (item != null) {
                    String[] parts = item.split(": ");
                    String currentName = parts[0];
                    String currentQuantity = parts[1];

                    Alert alert = new Alert(AlertType.CONFIRMATION);
                    alert.setTitle("Eliminar Artículo");
                    alert.setHeaderText("¿Estás seguro de eliminar este artículo?");
                    alert.setContentText("Nombre: " + currentName + "\nCantidad: " + currentQuantity);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        Document filter = new Document("name", currentName).append("quantity", currentQuantity);
                        collection.deleteOne(filter);
                        itemList.remove(cell.getItem());
                    }
                }
            });

            contextMenu.getItems().addAll(editItem, deleteItem);

            cell.textProperty().bind(cell.itemProperty());
            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell;
        });
    }

    @FXML
    protected void onHelloButtonClick() {
        String itemName = nombre.getText();
        String itemQuantity = cantidad.getText();
        if (!itemName.isEmpty() && !itemQuantity.isEmpty()) {
            Document doc = new Document("name", itemName)
                    .append("quantity", itemQuantity);
            collection.insertOne(doc);
            itemList.add(itemName + ": " + itemQuantity);
            nombre.clear();
            cantidad.clear();
        }
    }

    @FXML
    protected void onLoadProductsClick() {
        itemList.clear();
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String name = doc.getString("name");
                String quantity = doc.getString("quantity");
                itemList.add(name + ": " + quantity);
            }
        }
    }
}
