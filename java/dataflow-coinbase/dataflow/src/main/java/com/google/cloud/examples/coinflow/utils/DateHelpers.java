/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.examples.coinflow.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateHelpers {

  private static final Logger LOG = LoggerFactory.getLogger(DateHelpers.class);

  /**
   * Helper function that converts Coinbase timestamps to milliseconds
   * since epoch.
   * @param date in the Coinbase format to convert to milliseconds
   * @return The time in milliseconds since the epoch specified by the date
   */
  public static long convertDateToTime(String date) {
    // chop off Z at end
    String initialDate = new String(date);
    date = date.substring(0, date.length() - 4);
    DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    df1.setTimeZone(TimeZone.getTimeZone("UTC"));
    System.out.println("parsing date " + date);
    Date result;
    try {
      result = df1.parse(date);
      System.out.println("parsing date " + result);
    } catch (ParseException e) {
      LOG.error("Error trying to parse date: " + initialDate);
      LOG.error(e.getMessage());
      return -1;
    }
    return result.getTime();
  }
}
