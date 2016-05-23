var sensor_time = (data[317] << 8) + data[316];
var table = "currentRow: "+currentRow+"<br/>"+
"ts:"+sqlRows[0][1]+
(old_ts!==undefined?" delta: "+(ts-old_ts)+"s":"")+"<br/>"+
"sensortime:"+sensor_time+"<br/>"+
"<table width=\"100%\">";
for(var i =0; i < 40;i++)
{
	table +="<tr>";
	for(var j = 0;j < 8; j++)
	{
		var t = data[(i*8)+j];
		var oldt = old_data[(i*8)+j];
		table+="<td";
		if(t!=oldt)
		{
			table+=" style=\"color:red;\"";
		}
		table+=">";

		table+=hexChar[(t >> 4) & 0x0f]+" "+hexChar[t  & 0x0f];
		table+="</td>";
	}
	table +="</tr>";
}
table +="</table>";
return table;
---------------------------------------------------------------------------------
var table = "<table width=\"100%\">";
for(var i =0; i < 16;i++)
{
	table +="<tr><td>Trend["+i+"]:</td>";
	for(var j = 0;j < 6; j++)
	{
		var t = data[28+(i*6)+j];
		var oldt = old_data[28+(i*6)+j];
		
		table+="<td";
		if(t!=oldt)
		{
			table+=" style=\"color:red;\"";
		}
		table+=">";

		table+=hexChar[(t >> 4) & 0x0f]+" "+hexChar[t  & 0x0f];
		table+="</td>";
	}
	table +="</tr>";
}
for(var i =0; i < 32;i++)
{
	table +="<tr><td>Hist["+i+"]:</td>";
	for(var j = 0;j < 6; j++)
	{
		var t = data[124+(i*6)+j];
		var oldt = old_data[124+(i*6)+j];
		
		table+="<td";
		if(t!=oldt)
		{
			table+=" style=\"color:red;\"";
		}
		table+=">";

		table+=hexChar[(t >> 4) & 0x0f]+" "+hexChar[t  & 0x0f];
		table+="</td>";
	}
	table +="</tr>";
}
table +="</table>";
return table;

---------------------------------------------------------------------------------
var index_trend = data[26];
var index_historie = data[27];
var sensor_time = (data[317] << 8) + data[316];
var sensor_startts = ts - (sensor_time*60);
var sensor_endts = sensor_startts+ (14*24*60*60);
var time = new Date(ts * 1000);
var start = new Date(sensor_startts * 1000);
var end = new Date(sensor_endts * 1000);

var ret = "ts="+ts+"<br/>";
var soff = ts % 60;
ret += "soff="+soff+"<br/>";
var lastEntryTrend = ts - soff;
ret += "lastEntryTrend: ~"+lastEntryTrend+"<br/>";
var moff = sensor_time % 15;
ret += "moff="+moff+"<br/>";
var lastEntryHist = lastEntryTrend - (moff*60);
ret += "lastEntryHist: ~"+lastEntryHist+"<br/>";
ret += "nextEntryHist in "+(15-moff)+" minutes";
return ret;
---------------------------------------------------------------------------------
var sensor_time = (data[317] << 8) + data[316];
var soff = ts % 60;
var moff = sensor_time % 15;
var lastEntryTrend = ts - soff;
var lastEntryHist = lastEntryTrend - (moff*60);
var firstEntryHist = lastEntryHist - (8*15*60);

var ret ="<table width=\"100%\">";
for(var i = 0; i < csvCompareData.length ; i++)
{
	var t = csvCompareData[i];
	if(firstEntryHist < t.ts && lastEntryHist > t.ts)
	{
		ret +="<tr><td>TS: "+t.ts+"</td><td>ref: "+t.gluc+"</td></tr>";
	}
}
ret +="</table>";
return ret;