$(function() {
  function connect() {

    var options = {
        vertxbus_reconnect_attempts_max: Infinity, // Max reconnect attempts
        vertxbus_reconnect_delay_min: 1000, // Initial delay (in ms) before first reconnect attempt
        vertxbus_reconnect_delay_max: 5000, // Max delay (in ms) between reconnect attempts
        vertxbus_reconnect_exponent: 2, // Exponential backoff factor
        vertxbus_randomization_factor: 0.5 // Randomization factor between 0 and 1
    };

    var bus = new EventBus(window.location.protocol + '//' + window.location.hostname + '/eventbus', options);
    bus.enableReconnect(true);

    bus.onopen = function () {
        console.log('Connected');
        bus.registerHandler("updates", onMessage);
    }

    bus.onclose = function () {
        console.log('Disconnected');
    }
    
    function onMessage(err, res) {
      var data = res.body

      if (data.type == "country_stats") {
        updateMap(data);
      }
      else if (data.type == "total_orders") {
        updateTotals(data);
      }
      else if (data.type == "orders_per_currency_pairs") {
        updateCurrencyPairs(data);
      }
    }
  }

  function updateMap(mapStats) {
    mapdata = []
    stats = mapStats.stats
    for (var k in stats) {
      var countryData = stats[k]
      key = Object.keys(countryData)
      mapdata.push({code: key, z: countryData[key]})
    }
    var map = $("#map").highcharts();
    map.series[1].setData(mapdata);
    map.redraw();
  }

  function updateCurrencyPairs(currencyPairsStats) {
    currencyPairsData = []
    stats = currencyPairsStats.stats
    for (var k in stats) {
      var currencyData = stats[k]
      key = Object.keys(currencyData)
      console.log(currencyData)
      currencyPairsData.push({name: key, y: currencyData[key]})
    }
    var pairs = $("#currency-pairs").highcharts();
    pairs.series[0].setData(currencyPairsData);
    pairs.redraw();
  }

  function updateTotals(totalStats) {
    $("#orders-count").text(totalStats.stats[0].order);
  }

  var mapData = Highcharts.geojson(Highcharts.maps["custom/world"]);

  $("#map").highcharts("Map", {
    title: { text: "" },
    legend: { enabled: false },
    mapNavigation: { enabled: false },
    credits: { enabled: false },
    plotOptions: { series: { animation: { duration: 500 } } },
    series: [{
      name: "Countries",
      mapData: mapData,
      color: "#E0E0E0",
      enableMouseTracking: false
    }, {
      type: "mapbubble",
      mapData: mapData,
      name: "Number of orders",
      joinBy: ["iso-a2", "code"],
      minSize: 4,
      maxSize: "12%",
      tooltip: {
        pointFormat: "{point.code}: {point.z}"
      }
    }]
  });

  $("#currency-pairs").highcharts({
    credits: { enabled: false },
    chart: {
      plotBackgroundColor: null,
      plotBorderWidth: null,
      plotShadow: false,
      type: "pie"
    },
    title: { text: "" },
    plotOptions: {
      pie: {
        allowPointSelect: true,
        cursor: "pointer",
        size: "45%",
        dataLabels: {
          distance: 10,
          enabled: true,
          style: {
            color: "rgb(119,119,119)",
            textShadow: false
          }
        }
      }
    },
    series: [{
      name: "Orders per currency pair",
      colorByPoint: true
    }]
  });

  connect();
});
