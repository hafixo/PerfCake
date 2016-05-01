/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.reporting.destinations.c3chart;

import org.perfcake.PerfCakeException;
import org.perfcake.util.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class C3ChartHtmlTemplates {

   static String getJsImport(final String location) throws PerfCakeException {
      final Properties props = new Properties();
      props.setProperty("location", location);
      return Utils.readTemplateFromResource("/c3chart/js-import.html", props);
   }

   static String getChartDiv(final String baseName) throws PerfCakeException {
      final Properties props = new Properties();
      props.setProperty("baseName", baseName);
      return Utils.readTemplateFromResource("/c3chart/chart-div.html", props);
   }

   static String getHeading(final int level, final String id, final String heading) throws PerfCakeException {
      final Properties props = new Properties();
      props.setProperty("level", String.valueOf(level));
      props.setProperty("id", id);
      props.setProperty("heading", heading);
      return Utils.readTemplateFromResource("/c3chart/heading.html", props);
   }

   static String getTocEntry(final String id, final String label) throws PerfCakeException {
      final Properties props = new Properties();
      props.setProperty("id", id);
      props.setProperty("label", label);
      return Utils.readTemplateFromResource("/c3chart/toc-entry.html", props);
   }

   static String getToc(final Map<String, String> headings) throws PerfCakeException {
      final StringBuilder leftEntries = new StringBuilder();
      final StringBuilder rightEntries = new StringBuilder();
      final int half = headings.size() / 2 + headings.size() % 2;

      int counter = 0;
      for (final Map.Entry<String, String> entry : headings.entrySet()) {
         if (counter < half) {
            leftEntries.append(getTocEntry(entry.getKey(), entry.getValue()));
         } else {
            rightEntries.append(getTocEntry(entry.getKey(), entry.getValue()));
         }
         counter++;
      }

      final Properties props = new Properties();
      props.setProperty("leftEntries", leftEntries.toString());
      props.setProperty("rightEntries", rightEntries.toString());
      return Utils.readTemplateFromResource("/c3chart/toc.html", props);
   }

   static void writeIndex(final Path target, final List<C3Chart> charts) throws PerfCakeException {
      final Path indexFile = Paths.get(target.toString(), "index.html");
      final Properties indexProps = new Properties();
      indexProps.setProperty("js", getChartsImports(charts)); // javascript imports
      indexProps.setProperty("loader", getChartsScript(charts)); // javascript to render charts
      indexProps.setProperty("chart", getChartsDiv(charts)); // html content
      Utils.copyTemplateFromResource("/c3chart/index.html", indexFile, indexProps);
   }

   static String getChartsScript(final List<C3Chart> charts) throws PerfCakeException {
      final StringBuilder sb = new StringBuilder();
      for (final C3Chart chart : charts) {
         sb.append(getSingleChartScript(chart));
      }

      return sb.toString();
   }

   static String getSingleChartScript(final C3Chart chart) throws PerfCakeException {
      final Properties props = getChartProperties(chart);
      return Utils.readTemplateFromResource("/c3chart/chart.js", props);
   }

   static String getChartsImports(final List<C3Chart> charts) throws PerfCakeException {
      final StringBuilder sb = new StringBuilder();
      for (final C3Chart chart : charts) {
         sb.append(getJsImport("data/" + chart.getBaseName() + ".js"));
      }

      return sb.toString();
   }

   static String getChartsDiv(final List<C3Chart> charts) throws PerfCakeException {
      final StringBuilder sb = new StringBuilder();
      final List<String> groups = new ArrayList<>();
      final Map<String, List<C3Chart>> chartsByGroup = new HashMap<>();
      final Map<String, List<C3Chart>> chartsByGroupCombined = new HashMap<>();
      final Map<String, String> toc = new LinkedHashMap<>();

      for (final C3Chart chart : charts) {
         if (!groups.contains(chart.getGroup())) {
            groups.add(chart.getGroup());
         }

         if (chart.isCombined()) {
            chartsByGroupCombined.putIfAbsent(chart.getGroup(), new ArrayList<>());
            chartsByGroupCombined.get(chart.getGroup()).add(chart);
         } else {
            chartsByGroup.putIfAbsent(chart.getGroup(), new ArrayList<>());
            chartsByGroup.get(chart.getGroup()).add(chart);
         }
      }

      // list all groups
      for (final String group : groups) {
         sb.append(getHeading(2, "", "Charts for group: " + group));

         // plain charts, i.e. not created by combining others
         if (chartsByGroup.get(group) != null) {
            sb.append(getHeading(3, "", "Plain results"));

            for (final C3Chart chart : chartsByGroup.get(group)) {
               final String label = chart.getName() + " (created: " + getCreatedAsString(chart) + ")";
               sb.append(getHeading(4, chart.getBaseName(), label));
               sb.append(getChartDiv(chart.getBaseName()));
               toc.put(chart.getBaseName(), label);
            }
         }

         // combined charts next
         if (chartsByGroupCombined.get(group) != null) {
            sb.append(getHeading(3, "", "Combined results"));

            for (final C3Chart chart : chartsByGroupCombined.get(group)) {
               sb.append(getHeading(4, chart.getBaseName(), chart.getName()));
               sb.append(getChartDiv(chart.getBaseName()));
               toc.put(chart.getBaseName(), chart.getName());
            }
         }
      }

      sb.insert(0, getToc(toc));
      sb.insert(0, getHeading(2, "", "Table of Contents"));

      return sb.toString();
   }

   /**
    * Gets the creation date and time as localized string.
    *
    * @return The creation date and time as localized string.
    */
   public static String getCreatedAsString(final C3Chart chart) {
      final ZonedDateTime ldt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(chart.getCreated()), ZoneId.systemDefault());
      return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(ldt);
   }

   /**
    * Writes a quick view HTML file that can display the chart during the test run.
    *
    * @throws PerfCakeException
    *       When it was not possible to store the quick view file.
    */
   static void writeQuickView(final Path target, final C3Chart chart) throws PerfCakeException {
      final Path quickViewFile = Paths.get(target.toString(), "data", chart.getBaseName() + ".html");
      final Properties quickViewProps = getChartProperties(chart);
      Utils.copyTemplateFromResource("/c3chart/quick-view.html", quickViewFile, quickViewProps);
   }

   static Properties getChartProperties(final C3Chart chart) {
      final Properties props = new Properties();

      props.setProperty("baseName", chart.getBaseName());
      props.setProperty("xAxis", chart.getxAxis());
      props.setProperty("yAxis", chart.getyAxis());
      props.setProperty("chartName", chart.getName());
      props.setProperty("height", String.valueOf(chart.getHeight()));

      switch (chart.getxAxisType()) {
         case TIME:
            props.setProperty("format", "ms2hms");
            break;
         case ITERATION:
            props.setProperty("format", "function(x) { return x; }");
            break;
         case PERCENTAGE:
            props.setProperty("format", "function(x) { return '' + x + '%'; }");
            break;
      }

      return props;
   }
}