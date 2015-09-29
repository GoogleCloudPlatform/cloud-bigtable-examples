/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


angular.module('coinflow', ['nvd3'])
    .controller('myCtrl', function($scope, $http) {

        $scope.options = {
            chart: {
                type: 'lineChart',
                height: 450,
                margin: {
                    top: 20,
                    right: 20,
                    bottom: 40,
                    left: 80
                },
                x: function (d) {
                    return d.timestamp;
                },
                y: function (d) {
                    return d.price;
                },
                xAxis: {
                    axisLabel: 'Time',
                    tickFormat: function(t) {
                        var date = new Date(t);
                        var hours = date.getHours();
                        var minutes = "0" + date.getMinutes();
                        var seconds = "0" + date.getSeconds();
                        var formattedTime = hours + ':' + minutes.substr(-2) + ':'
                            + seconds.substr(-2);
                        return formattedTime;
                    },
                    tickLength: 30
                },
                yAxis: {
                    axisLabel: 'Price (USD)',
                    tickFormat: function(p) {
                        return "$" + p;
                    }
                },
                callback: function (chart) {
                    console.log("!!! lineChart callback !!!");
                }
            }
        };

        $http({
            method: 'GET',
            url: '/coinflow'
        }).then(function success(response) {
            document.getElementById("spinner").style.display = "none";
            $scope.data = [{values: response.data, color: "#ff7f0e", key: "Trades"}];
        }, function error(response) {
           console.log("Failed!");
            console.log(response);
        });
    });

