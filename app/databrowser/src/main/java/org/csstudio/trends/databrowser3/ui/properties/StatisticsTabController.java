/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.csstudio.trends.databrowser3.ui.properties;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.model.PlotSamples;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import org.phoebus.util.time.TimeRelativeInterval;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tab showing statistics data for traces. User needs to actively request computation of the statistical data.
 * Only samples visible in the plot are considered in the calculation. For instance, if the time axis changes
 * to show a subset of loaded samples, the calculation will not consider non-visible portions of the trace.
 */
public class StatisticsTabController implements ModelListener{
    private Model model;

    private static Color colorIndicatorBorderColor =
            Color.color(0.71, 0.71, 0.71);

    @FXML
    private Button refreshAll;

    @FXML
    private TableView<ModelItemStatistics> tracesTable;
    @FXML
    private TableColumn<ModelItemStatistics, ColorIndicator> indicatorColumn;
    @FXML
    private TableColumn<ModelItemStatistics, String> displayNameColumn;
    @FXML
    private TableColumn<ModelItemStatistics, String> countColumn;
    @FXML
    private TableColumn<ModelItemStatistics, String> meanColumn;
    @FXML
    private TableColumn<ModelItemStatistics, String> medianColumn;
    @FXML
    private TableColumn<ModelItemStatistics, String> stdDevColumn;
    @FXML
    private TableColumn<ModelItemStatistics, String> minColumn;
    @FXML
    private TableColumn<ModelItemStatistics, String> maxColumn;
    @FXML
    private TableColumn<ModelItemStatistics, String> sumColumn;

    public StatisticsTabController(Model model){
        this.model = model;
    }


    @FXML
    public void initialize() {

        refreshAll.setText(Messages.Refresh);
        refreshAll.setOnAction(e -> refreshAll());
        tracesTable.setPlaceholder(new Label(Messages.TraceTableEmpty));
        model.addListener(this);
        createTable();
    }

    @Override
    public void itemAdded(ModelItem modelItem) {
        ModelItemStatistics statistics = new ModelItemStatistics(modelItem);
        tracesTable.getItems().add(statistics);
    }

    @Override
    public void changedItemLook(ModelItem modelItem){
        ModelItemStatistics statistics =
                tracesTable.getItems().stream().filter(item -> item.getUniqueItemId().equals(modelItem.getUniqueId())).findAny().orElse(null);
        if(statistics != null){
            statistics.setTraceName(modelItem.getResolvedDisplayName());
            statistics.setColorIndicator(modelItem.getPaintColor());
        }
    }

    @Override
    public void itemRemoved(ModelItem modelItem) {
        ModelItemStatistics statistics =
                tracesTable.getItems().stream().filter(item -> item.getUniqueItemId().equals(modelItem.getUniqueId())).findAny().orElse(null);
        if(statistics != null){
            tracesTable.getItems().remove(statistics);
        }
    }

    @FXML
    public void refreshAll(){
        tracesTable.getItems().stream().forEach(item -> item.update(model.getItemByUniqueId(item.getUniqueItemId())));
    }

    private void createTable(){
        displayNameColumn.setText(Messages.TraceDisplayName);
        displayNameColumn.setCellValueFactory(cell -> cell.getValue().getTraceName());
        displayNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        indicatorColumn.setCellValueFactory(column -> column.getValue().getColorIndicator());

        indicatorColumn.setCellFactory(column -> new TableCell<>(){
            @Override
            public void updateItem(final ColorIndicator colorIndicator, final boolean empty){
                super.updateItem(colorIndicator, empty);
                setGraphic(empty ? null : colorIndicator);
                setStyle( "-fx-alignment: CENTER-LEFT;");
            }
        });

        countColumn.setText(Messages.StatisticsSampleCount);
        countColumn.setCellValueFactory(cell -> cell.getValue().getCount());
        meanColumn.setText(Messages.StatisticsMean);
        meanColumn.setCellValueFactory(cell -> cell.getValue().getMean());
        medianColumn.setText(Messages.StatisticsMedian);
        medianColumn.setCellValueFactory(cell -> cell.getValue().getMedian());
        stdDevColumn.setText(Messages.StatisticsStdDev);
        stdDevColumn.setCellValueFactory(cell -> cell.getValue().getStdDev());
        minColumn.setText(Messages.StatisticsMin);
        minColumn.setCellValueFactory(cell -> cell.getValue().getMin());
        maxColumn.setText(Messages.StatisticsMax);
        maxColumn.setCellValueFactory(cell -> cell.getValue().getMax());
        sumColumn.setText(Messages.StatisticsSum);
        sumColumn.setCellValueFactory(cell -> cell.getValue().getSum());
    }

    public void removeModelListener(){
        model.removeListener(this);
    }

    /**
     * Simple color indicator used to identify the trace in the table.
     */
    private class ColorIndicator extends Rectangle {
        public ColorIndicator(Color color){
            super();
            setX(0);
            setY(0);
            setWidth(12);
            setHeight(12);
            setFill(color);
            setStroke(colorIndicatorBorderColor);
        }
    }

    /**
     * Wraps statistical data plus name and color indicator for a trace. This is
     * the data model for the table.
     */
    private class ModelItemStatistics {
        private SimpleStringProperty count = new SimpleStringProperty();
        private SimpleStringProperty mean = new SimpleStringProperty();
        private SimpleStringProperty median = new SimpleStringProperty();
        private SimpleStringProperty stdDev = new SimpleStringProperty();
        private SimpleStringProperty min = new SimpleStringProperty();
        private SimpleStringProperty max = new SimpleStringProperty();
        private SimpleStringProperty sum = new SimpleStringProperty();
        private SimpleObjectProperty colorIndicator = new SimpleObjectProperty();
        private SimpleStringProperty traceName = new SimpleStringProperty();

        private String uniqueItemId;

        public ModelItemStatistics(ModelItem modelItem){
            uniqueItemId = modelItem.getUniqueId();
            setColorIndicator(modelItem.getPaintColor());
            setTraceName(modelItem.getResolvedDisplayName());
        }

        /**
         * Updates the statistics data using background {@link org.phoebus.framework.jobs.Job}.
         * @param modelItem
         */
        public void update(final ModelItem modelItem){
            if(modelItem == null){ // Should not happen...?
                return;
            }
            DescriptiveStatistics statistics = new DescriptiveStatistics();

            JobManager.schedule("Compute trace statistics", new JobRunnable() {
                @Override
                public void run(JobMonitor monitor){
                    PlotSamples plotSamples = modelItem.getSamples();
                    plotSamples.getLock().lock();
                    TimeRelativeInterval timeRelativeInterval = model.getTimerange();
                    long start;
                    long end;
                    if(timeRelativeInterval.getAbsoluteEnd().isPresent()){
                        start = 1000 * timeRelativeInterval.getAbsoluteStart().get().getEpochSecond();
                        end = 1000 *  timeRelativeInterval.getAbsoluteEnd().get().getEpochSecond();
                    }
                    else{
                        long now = System.currentTimeMillis();
                        start = now - 1000 * timeRelativeInterval.getRelativeStart().get().get(ChronoUnit.SECONDS);
                        end = now - 1000 * timeRelativeInterval.getRelativeEnd().get().get(ChronoUnit.SECONDS);
                    }
                    int length = modelItem.getSamples().size();
                    for(int i = 0; i < length; i++){

                        if(plotSamples.get(i).getPosition().isBefore(Instant.ofEpochMilli(start))){
                            continue;
                        }
                        else if(plotSamples.get(i).getPosition().isAfter(Instant.ofEpochMilli(end))){
                            break;
                        }
                        statistics.addValue(plotSamples.get(i).getValue());
                    }
                    plotSamples.getLock().unlock();
                    count.set(String.valueOf(statistics.getN()));
                    mean.set(String.valueOf(statistics.getMean()));
                    median.set(String.valueOf(statistics.getPercentile(50)));
                    stdDev.set(String.valueOf(statistics.getStandardDeviation()));
                    min.set(String.valueOf(statistics.getMin()));
                    max.set(String.valueOf(statistics.getMax()));
                    sum.set(String.valueOf(statistics.getSum()));
                }
            });
        }

        public void setTraceName(String traceName){
            this.traceName.set(traceName);
        }

        public void setColorIndicator(Color color){
            colorIndicator.set(new ColorIndicator(color));
        }

        public SimpleStringProperty getCount() {
            return count;
        }

        public SimpleStringProperty getStdDev() {
            return stdDev;
        }

        public SimpleStringProperty getMean() {
            return mean;
        }

        public SimpleStringProperty getMedian() {
            return median;
        }

        public SimpleStringProperty getMin() {
            return min;
        }

        public SimpleStringProperty getMax() {
            return max;
        }

        public SimpleStringProperty getSum() {
            return sum;
        }

        public SimpleObjectProperty getColorIndicator() {
            return colorIndicator;
        }

        public SimpleStringProperty getTraceName() {
            return traceName;
        }

        public String getUniqueItemId(){
            return uniqueItemId;
        }

        @Override
        public boolean equals(Object other){
            if(!(other instanceof ModelItemStatistics)){
                return false;
            }
            return uniqueItemId.equals(((ModelItemStatistics)other).getUniqueItemId());
        }

        @Override
        public int hashCode(){
            return uniqueItemId.hashCode();
        }
    }
}