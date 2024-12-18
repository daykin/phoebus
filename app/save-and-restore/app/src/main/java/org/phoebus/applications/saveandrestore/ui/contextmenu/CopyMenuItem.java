/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.contextmenu;

import javafx.collections.ObservableList;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.ui.javafx.ImageCache;

import java.util.function.Consumer;

public class CopyMenuItem extends SaveAndRestoreMenuItem {

    public CopyMenuItem(SaveAndRestoreController saveAndRestoreController, ObservableList<Node> selectedItemsProperty, Runnable onAction) {
        super(saveAndRestoreController, selectedItemsProperty, onAction);
        setText(Messages.copy);
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));
    }

    @Override
    public void configure() {
        disableProperty().set(saveAndRestoreController.getUserIdentity().isNull().get() ||
                allFoldersOrRootFolder(selectedItemsProperty) ||
                !saveAndRestoreController.mayCopy());
    }

    private boolean allFoldersOrRootFolder(ObservableList<Node> selectedItemsProperty) {
        return selectedItemsProperty.stream().anyMatch(n -> n.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) ||
                selectedItemsProperty.stream().filter(n -> n.getNodeType().equals(NodeType.FOLDER)).count() == selectedItemsProperty.size();
    }
}
