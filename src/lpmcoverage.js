var modelcoverage = coverageData();

var colwidth = 45;
var rowheight = 2;
var nocovcolor = '#cccccc';
var covcolor = '#4ea96c';
var multicovcolor = '#880088';

var validdata = false;
var numtraces = 0;
var nummodels = 0;
var numevents = 0;

function checkData() {
    if (validdata) {
        return true;
    }
    
    numtraces = modelcoverage.length;
    if (numtraces < 1) {
        alert ("No traces found");
        return false;
    }
    
    nummodels = modelcoverage[0]['coverage'].length;
    for (var i = 0; i < numtraces; i++) {
        if (modelcoverage[i]['coverage'].length != nummodels) {
            alert ("Inconsistent number of models");
            return false;
        }
    }
    if (nummodels < 1) {
        alert ("No models found");
        return false;
    }
    
    for (var i = 0; i < numtraces; i++) {
        var tracelength = modelcoverage[i]['coverage'][0].length;
        for (var j = 0; j < nummodels; j++) {
            if (modelcoverage[i]['coverage'][j].length != tracelength) {
                alert ("Inconsistent number of events");
                return false;
            }
        }
        if (tracelength < 1) {
            alert ("Warning: Empty trace found");
        }
        if (tracelength > 15000) {
            alert ("Warning: Trace with length > 15k found. This may not be visualized properly.");
        }
        numevents += tracelength * modelcoverage[i]['multiplicity'];
    }
    
    validdata = true;
    return true;
}

function formatInt(num) {
  	if (num < 1000) {
      	return num;
    }
  	num /= 1000;
  	var letters = ['k', 'M', 'G'];
  	var letter = 0;
  	while (num.toPrecision(3) >= 1000) {
      	letter++;
      	num /= 1000;
    }
    return num.toPrecision(3) + letters[letter];
}

function load() {
    // check some essentials
    if (!checkData()) {
        return;
    }
    
    // create table with a number and checkbox for each model
	var table = document.getElementById('coverage');
    var tablerow = table.rows[0];
    for (var col = 0; col < nummodels; ) {
        col++;
        var th = document.createElement('th');
        th.innerHTML = col + "<input type=\"checkbox\" checked id=\"cb" + col + "\" />";
        tablerow.appendChild(th);
    }
    // finish table with a combine button
    var th = document.createElement('th');
    th.innerHTML = "<input type=\"button\" value=\"combine\" onclick=\"combine();\" />"
        + "<input type=\"button\" value=\"I/O\" onclick=\"inout();\" />";
    tablerow.appendChild(th);
    
    // add other statistics
    for (var key in stats) {
        if (!stats.hasOwnProperty(key)) {
            //The current property key is not a direct property of stats
            continue;
        }
        var keyparts = key.split('_');
        tablerow = document.createElement('tr');
        for (var col = 0; col < nummodels; col++) {
            var cell = document.createElement('td');
            if (keyparts[0] == 'double') {
                cell.innerHTML = stats[key][col].toFixed(3);
                cell.title = stats[key][col];
            } else {
                cell.innerHTML = formatInt(stats[key][col]);
                cell.title = stats[key][col];
            }
            cell.className = "num";
            tablerow.appendChild(cell);
        }
        var cell = document.createElement('td');
        cell.innerHTML = keyparts[1];
        tablerow.appendChild(cell);
        table.appendChild(tablerow);
    }
    
    // add coverage statistics
    var coveragestats = [];
    for (var col = 0; col < nummodels; col++) {
        coveragestats.push(0);
    }
    for (var i = 0; i < numtraces; i++) {
        var mult = modelcoverage[i]['multiplicity'];
        for (var col = 0; col < nummodels; col++) {
            var trace = modelcoverage[i]['coverage'][col];
            for (var j = 0; j < trace.length; j++) {
                if (trace[j]) {
                    coveragestats[col] += mult;
                }
            }
        }
    }
    tablerow = document.createElement('tr');
    for (var col = 0; col < nummodels; col++) {
        var cell = document.createElement('td');
        cell.innerHTML = formatInt(coveragestats[col]) + "<br />"
            + (coveragestats[col] / numevents).toFixed(3);
        cell.title = coveragestats[col] + " / " + numevents + " (" + (coveragestats[col] / numevents) + ")";
        cell.className = "num";
        tablerow.appendChild(cell);
    }
    var cell = document.createElement('td');
    cell.id = "total_coverage";
    cell.className = "num";
    tablerow.appendChild(cell);
    table.appendChild(tablerow);
    
    
    // prepare canvases
    var canvases = document.getElementById('canvases');
    var canvaswidth = nummodels * colwidth;
    
    for (var i = 0; i < numtraces; i++) {
        var trace = modelcoverage[i]
        var canvasheight = trace['coverage'][0].length * rowheight;
    
        var newdiv = document.createElement('div');
        newdiv.innerHTML = "<canvas id=\"models_trace" + i + "\" width=\"" + canvaswidth + "\" height=\"" + canvasheight + "\"></canvas>" +
        "<canvas id=\"totals_trace" + i + "\" width=\"" + colwidth + "\" height=\"" + canvasheight + "\"></canvas>" +
        "<div title=\"" + trace['multiplicity'] + "\">&times;" + formatInt(trace['multiplicity']) + "</div><br />";
        canvases.appendChild(newdiv);
    }
    
    drawModels();
    combine();
}

function drawModels() {
    if (!checkData()) {
        return;
    }
    
    for (var i = 0; i < numtraces; i++) {
        var coveragereport = modelcoverage[i]['coverage'];
        
        var canvas = document.getElementById('models_trace' + i);
        if (!canvas.getContext) {
            return;
        }
        var ctx = canvas.getContext('2d');
        
        var numevents = coveragereport[0].length;
        for (var col = 0; col < nummodels; col++) {
            for (var row = 0; row < numevents; row++) {
                if (!coveragereport[col][row]) {
                    ctx.fillStyle = nocovcolor;
                } else {
                    ctx.fillStyle = covcolor;
                }
                ctx.fillRect(col * colwidth, row * rowheight, colwidth - 1, rowheight);
            }
        }
    }
}

function getColumns() {
	var columns = [];
	
    for (var i = 0; i < nummodels; i++) {
		var input = document.getElementById('cb' + (i + 1));
        if (input.checked == true) {
            columns.push(i);
        }
    }
	return columns;
}

function inout() {
	var columns = [];
    for (var i = 1; i <= nummodels; i++) {
		var input = document.getElementById('cb' + (i));
        if (input.checked == true) {
            columns.push(i);
        }
    }
    columns = window.prompt("Numbers and commas only; no spaces.", columns.join()).split(',');
    for (var i = 1; i <= nummodels; i++) {
		var input = document.getElementById('cb' + (i));
        input.checked = false;
    }
    for (var i = 0; i < columns.length; i++) {
        var input = document.getElementById('cb' + (columns[i]));
        input.checked = true;
    }
}

function combine() {
    if (!checkData()) {
        return;
    }
    
    var columns = getColumns();
    var tot = 0;
    var dup = 0;
    
    for (var i = 0; i < numtraces; i++) {
        var coveragereport = modelcoverage[i]['coverage'];
        
        var canvas = document.getElementById('totals_trace' + i);
        if (!canvas.getContext) {
            return;
        }
        var ctx = canvas.getContext('2d');
        
        
        var tracelength = coveragereport[0].length;
        for (var row = 0; row < tracelength; row++) {
            var value = 0;
            for (var j = 0; j < columns.length; j++) {
                value += coveragereport[columns[j]][row];
            }
            if (value > 1) {
                tot += modelcoverage[i]['multiplicity'];
                dup += modelcoverage[i]['multiplicity'];
                ctx.fillStyle = multicovcolor;
            } else if (value == 0) {
                ctx.fillStyle = nocovcolor;
            } else {
                tot += modelcoverage[i]['multiplicity'];
                ctx.fillStyle = covcolor;
            }
            ctx.fillRect(0, row * rowheight, colwidth - 1, rowheight);
        }
    }
    
    var cell = document.getElementById('total_coverage');
    cell.innerHTML = "Total: " + formatInt(tot) + "<br />Duplicate: " + formatInt(dup)
        + "<br />Total: " + (tot / numevents).toFixed(3)
        + "<br />Duplicate: " + (dup / numevents).toFixed(3);
    cell.title = "Total: " + tot + " / " + numevents + " (" + (tot / numevents) + ")\r\nDuplicate: " + dup + " / " + numevents + " (" + (dup / numevents) + ")";
}