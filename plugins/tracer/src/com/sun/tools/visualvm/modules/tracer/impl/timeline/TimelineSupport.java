/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 * All methods must be invoked from the EDT.
 *
 * @author Jiri Sedlacek
 */
public final class TimelineSupport {

    private final TimelineChart chart;
    private final TimelineModel model;
    private final SynchronousXYItemsModel itemsModel;

    private TimelineTooltipOverlay tooltips;

    private final List<TracerProbe> probes = new ArrayList();
    private final List<TimelineChart.Row> rows = new ArrayList();


    // --- Constructor ---------------------------------------------------------

    public TimelineSupport() {
        // TODO: must be called in EDT!
        model = new TimelineModel();
        itemsModel = new SynchronousXYItemsModel(model);
        chart = new TimelineChart(itemsModel);
        tooltips = new TimelineTooltipOverlay(chart);
        chart.addOverlayComponent(tooltips);
    }


    // --- Chart access --------------------------------------------------------

    TimelineChart getChart() {
        return chart;
    }


    // --- Probes management ---------------------------------------------------

    public void addProbe(final TracerProbe probe) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TimelineChart.Row row = chart.addRow();

                probes.add(probe);
                rows.add(row);

                ProbeItemDescriptor[] itemDescriptors = probe.getItemDescriptors();
                TimelineXYItem[] items = model.createItems(itemDescriptors);
                XYItemPainter[] painters  = new XYItemPainter[items.length];
                for (int i = 0; i < painters.length; i++)
                    painters[i] = TimelinePaintersFactory.createPainter(
                            itemDescriptors[i], i);
                
                row.addItems(items, painters);

                setupTooltips();
            }
        });
    }

    public void removeProbe(final TracerProbe probe) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TimelineChart.Row row = getRow(probe);

                chart.removeRow(row);
                
                model.removeItems(row.getItems());

                rows.remove(row);
                probes.remove(probe);

                setupTooltips();
            }
        });
    }

    public List<TracerProbe> getProbes() {
        return probes;
    }

    public int getItemsCount() {
        return model.getItemsCount();
    }


    // --- Tooltips support ----------------------------------------------------

    private void setupTooltips() {
        TimelineTooltipPainter[] ttPainters = new TimelineTooltipPainter[chart.getRowsCount()];
        for (int i = 0; i < ttPainters.length; i++) {
            final TimelineChart.Row row = chart.getRow(i);
            final TracerProbe probe = getProbe(row);
            ttPainters[i] = new TimelineTooltipPainter(new TimelineTooltipModel() {

                public int getRowsCount() {
                    return row.getItemsCount();
                }

                public String getRowName(int index) {
                    return ((TimelineXYItem)row.getItem(index)).getName();
                }

                public String getRowValue(int index, long itemValue) {
                    return Long.toString(itemValue);
                }

                public String getRowUnits(int index) {
                    ProbeItemDescriptor d = probe.getItemDescriptors()[index];
                    if (d instanceof ProbeItemDescriptor.ValueItem)
                        return ((ProbeItemDescriptor.ValueItem)d).getUnitsString();
                    else
                        return null; // NOI18N
                }

            });
        }
        tooltips.setupPainters(ttPainters);
    }


    // --- Rows <-> Probes mapping ---------------------------------------------

    TimelineChart.Row getRow(TracerProbe probe) {
        return rows.get(probes.indexOf(probe));
    }

    TracerProbe getProbe(TimelineChart.Row row) {
        return probes.get(rows.indexOf(row));
    }


    // --- Values management ---------------------------------------------------

    public void addValues(final long timestamp, final long[] newValues) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                model.addValues(timestamp, newValues);
                itemsModel.valuesAdded();
            }
        });
    }

    public void resetValues() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                model.reset();
                itemsModel.valuesReset();
            }
        });
    }

}
