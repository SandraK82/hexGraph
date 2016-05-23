package de.sandrakessler.libredata.client;

import java.util.ArrayList;

import org.vectomatic.file.File;
import org.vectomatic.file.FileList;
import org.vectomatic.file.FileReader;
import org.vectomatic.file.FileUploadExt;
import org.vectomatic.file.events.ErrorEvent;
import org.vectomatic.file.events.ErrorHandler;
import org.vectomatic.file.events.LoadEndEvent;
import org.vectomatic.file.events.LoadEndHandler;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.TypedArrays;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

class Compare extends JavaScriptObject{
	protected Compare() {}

	  public static native Compare create(int ts, int type, int gluc) /*-{
	    return {ts: ts,type: type, gluc: gluc};
	  }-*/;
	
}

public class HexGraph implements EntryPoint {
	private static String CALC_SPLIT = "---------------------------------------------------------------------------------";
	
	private static final String[] hexChar = {"0", "1", "2", "3", "4", "5", "6", "7","8", "9", "A", "B", "C", "D", "E", "F"};
	
	private HorizontalPanel columns = new HorizontalPanel();
	private VerticalPanel header = new VerticalPanel();
	
	private VerticalPanel configRows = new VerticalPanel();
	
	private HorizontalPanel sqlUploadPanel;
	private HorizontalPanel csvUploadPanel;

	private JsArrayMixed sqlRows;
	private Compare[] csvCompareData = new Compare[0];
	
	private String[] blocks = new String[0];

	private TextBox start;

	private TextBox count;
	
	public void onModuleLoad() {
		header = new VerticalPanel();
		
		HorizontalPanel page = new HorizontalPanel();
		page.add(configRows);
		page.add(header);
		RootPanel.get().add(page);
		
		
		loadConfiguration();
		
	}
	
	private void loadConfiguration() {
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode("./calc.js"));
		try {
			builder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					GWT.log("request:" + exception);
					makeConfigPanel(false);

					makeSQLUploadPanel();
				}

				public void onResponseReceived(Request request,
						Response response) {
					if (200 == response.getStatusCode()) {
						String raw = response.getText();
						parseRawConfig(raw);
						
					} else {
						// Handle the error. Can get the status text from
						// response.getStatusText()
					}
					makeConfigPanel(false);

					makeSQLUploadPanel();
				}
			});
		} catch (RequestException e) {
			GWT.log("requestBuilder:" + e);
			makeConfigPanel(false);

			makeSQLUploadPanel();
		}
	}

	private void parseRawConfig(String raw)
	{
		String[] resp = raw.split(CALC_SPLIT);
		
		ArrayList<String> jsL = new ArrayList<String>();
		for(int i = 0;i < resp.length;i++)
		{
			if(resp[i] != null && resp[i].trim().length()>0)
			{
				jsL.add(resp[i]);
			}
		}
		blocks = new String[jsL.size()];
		for(int i = 0; i < jsL.size();i++)
		{
			blocks[i] = jsL.get(i);
		}
	}
	
	private String getRawConfig()
	{
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < blocks.length;i++)
		{
			b.append(blocks[i]);
			b.append(CALC_SPLIT);
		}
		return b.toString();
		
	}
	private void makeSQLUploadPanel() {
		sqlUploadPanel = new HorizontalPanel();
		final FileUploadExt uploadFile = new FileUploadExt();
		uploadFile.setName("file");
		sqlUploadPanel.add(new Label("Select db:"));
		sqlUploadPanel.add(uploadFile);
		uploadFile.addChangeHandler(new ChangeHandler() {
			
			
			@Override
			public void onChange(ChangeEvent event) {
				FileList list = uploadFile.getFiles();
				for(int i = 0; i < list.getLength();i++)
				{
					File file = list.getItem(i);
					readSQLFile(file);
				}
			}
			
			private void readSQLFile(File file) {
				System.err.println(file.getName());
				//Window.alert(file.getName());
				final FileReader reader = new  FileReader();
				reader.addLoadEndHandler(new LoadEndHandler() {
					
					@Override
					public void onLoadEnd(LoadEndEvent event) {
						ArrayBuffer ab = reader.getArrayBufferResult();
						sqlFinishedLoading(ab);
					}
					

					
				});
				reader.addErrorHandler(new ErrorHandler() {
					
					@Override
					public void onError(ErrorEvent event) {
						GWT.log("read error: "+event);
					}
				});
				reader.readAsArrayBuffer(file);
			}
		});
		header.add(sqlUploadPanel);
	}

	private static native JavaScriptObject openDB(Uint8Array buf) /*-{
	  var sql = $wnd.SQL;
	  var db = new sql.Database(buf);
	 return db;
	}-*/;
	private native JavaScriptObject execute(String query,JavaScriptObject db) /*-{
		 var contents = db.exec(query);
	  return contents;
	}-*/;
	private native String getString0( JsArrayMixed jso, int index )
	/*-{
		var value = jso[index];
		return (value==null||value==undefined) ? null : value;
	}-*/;
	private void csvFinishedLoading(String csvdata) 
	{
		String[] lines = csvdata.split("\n");
		DateTimeFormat dtf = DateTimeFormat.getFormat("yyyy.MM.dd HH:mm");
		GWT.log("Got "+lines.length+" lines");
		ArrayList<Compare> data = new ArrayList<Compare>();
		for(int i = 1; i < lines.length;i++)
		{
			try
			{
				String parts[] = lines[i].split("\t");
				long ts = dtf.parse(parts[1]).getTime()/1000;
				int type = Integer.parseInt(parts[2]);
				int gluc = 0;
				if(type == 0)
				{
					gluc = Integer.parseInt(parts[3]);
				}
				else if(type == 1)
				{
					gluc = Integer.parseInt(parts[4]);
				}
				if(type == 0 || type== 1)
				{
					GWT.log("l"+i+" ts:"+ts+" type:"+type+" gluc:"+gluc);
					Compare c = Compare.create((int)ts, type, gluc);
					data.add(c);
				}
			}catch(Throwable th)
			{
				//ignore me
			}
		}
		csvCompareData = new Compare[data.size()];
		for(int i = 0; i < data.size();i++)
		{
			csvCompareData[i] = data.get(i);
		}
	
	}
	private void sqlFinishedLoading(ArrayBuffer ab) {

		sqlUploadPanel.setVisible(false);
		
		Uint8Array buf = TypedArrays.createUint8Array(ab,0,ab.byteLength());
		JavaScriptObject db = openDB(buf);
		JavaScriptObject jso = execute("SELECT * FROM \"raw_data\" order by timestamp ASC", db);
		
		JSONValue r = new JSONArray( jso ).get( 0 );
		if( r == null )
			return;
		
		JSONObject root = r.isObject();
		
		sqlRows = ((JsArrayMixed)root.get( "values" ).isArray().getJavaScriptObject().cast());
		
		addCSVForm();
		addDataForm();
		HorizontalPanel dataPanel = new HorizontalPanel();
		
		columns = new HorizontalPanel();
		columns.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		columns.setSpacing(1);
		
		dataPanel.add(columns);
		header.add(dataPanel);
	
	}
	private void makeConfigPanel(boolean expanded) {
		configRows.clear();
		if(!expanded)
		{
			configRows.setHeight("100%");
			final Button expand = new Button(">");
			expand.getElement().getStyle().setProperty("minHeight", "300px");
			expand.setHeight("100%");
			expand.getElement().getStyle().setColor("black");
			expand.addClickHandler(new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					makeConfigPanel(true);
				}

			});
			configRows.add(expand);
		}
		else
		{
			final ArrayList<TextArea> blockTexts = new ArrayList<TextArea>();
			Button collapse = new Button("< apply");
			collapse.getElement().getStyle().setProperty("minWidth", "200px");
			collapse.getElement().getStyle().setColor("black");
			collapse.addClickHandler(new ClickHandler() {
				
				@Override
				public void onClick(ClickEvent event) {
					ArrayList<String> jsL = new ArrayList<String>();
					for(TextArea blockText : blockTexts)
					{
						String js = blockText.getText();
						if(js.trim().length()>0)
						{
							jsL.add(js);
						}
					}
					blocks = new String[jsL.size()];
					for(int i = 0;i < jsL.size();i++)
					{
						blocks[i] = jsL.get(i);
						GWT.log("block["+i+"]="+jsL.get(i));
					}
					makeConfigPanel(false);
					makeDataPanels();
				}
			});

			final FileUploadExt load = new FileUploadExt();
			load.addChangeHandler(new ChangeHandler() {
				
				@Override
				public void onChange(ChangeEvent event) {
					FileList list = load.getFiles();
					for(int i = 0; i < list.getLength();i++)
					{
						File file = list.getItem(i);
						final FileReader reader = new  FileReader();
						reader.addLoadEndHandler(new LoadEndHandler() {
							
							@Override
							public void onLoadEnd(LoadEndEvent event) {
								String raw = reader.getStringResult();
								GWT.log("got raw:"+raw);
								parseRawConfig(raw);
								makeConfigPanel(true);
							}
						});
						reader.addErrorHandler(new ErrorHandler() {
							
							@Override
							public void onError(ErrorEvent event) {
								GWT.log("read error: "+event);
							}
						});
						reader.readAsText(file);
					}
				}
			});
			load.getElement().getStyle().setColor("black");
			
			Button save = new Button("save");
			save.getElement().getStyle().setProperty("minWidth", "200px");
			save.getElement().getStyle().setColor("black");
			
			configRows.add(collapse);
			HorizontalPanel line = new HorizontalPanel();
			line.add(load);
			line.add(save);
			configRows.add(line);
			
			for(int i = 0; i <= blocks.length;i++)
			{
				blockTexts.add(addConfigBlock(i));
			}
			
			save.addClickHandler(new ClickHandler() {
				
				@Override
				public void onClick(ClickEvent event) {
					ArrayList<String> jsL = new ArrayList<String>();
					for(TextArea blockText : blockTexts)
					{
						String js = blockText.getText();
						if(js.trim().length()>0)
						{
							jsL.add(js);
						}
					}
					StringBuilder raw =new StringBuilder();
					for(int i = 0;i < jsL.size();i++)
					{
						String t = jsL.get(i);
						raw.append(t);
						if(!t.endsWith("\n"))
							raw.append('\n');
						raw.append(CALC_SPLIT);
					}
					
					String r = URL.encodeComponent(raw.toString(),false);
					Window.open("data:text/plain;charset=utf8,"+r, "vis.js", "");
				}
			});
		}
	}
	private TextArea addConfigBlock(int index) {
		final TextArea text = new TextArea();
		text.setCharacterWidth(75);
		text.setHeight("250px");
		text.getElement().getStyle().setColor("black");
		if(index < blocks.length)
			text.setText(blocks[index]);
		configRows.add(text);
		Button check = new Button("test");
		check.getElement().getStyle().setColor("black");
		check.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				try
				{
					String html = evalBlock(text.getText(),sqlRows,csvCompareData,1);
					Window.alert("Result: "+html);
				}
				catch(Throwable th)
				{
					Window.alert("Exception: "+th);
				}
			}
		});
		
		Button help = new Button("Help");
		help.getElement().getStyle().setColor("black");
		help.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				Window.alert("Define Blocks for Display.\n"
						+ "The Code will be called for each DB-Record.\n"
						+ "Predefined Vars:\n"
						+ "sqlRows: data from DB (mixed Array [0]:id, [1]:ts, [2]:rawData, [3]:1, [4]:tagUID)\n"
						+ "currentRow: current Row for block (int)\n"
						+ "csvCompareData: if loaded libre export Data (.ts timestamp(long), .gluc glucose reading(int), .type 0==auto, 1== scan)\n"
						+ "row: sqlRows[currentRow];\n"
						+ "old_row = currentRow>0?sqlRows[currentRow-1]:undefined;\n"
						+ "ts = row[1];\n"
						+ "old_ts = old_row!==undefined?old_row[1]:undefined;\n"
						+ "data = row[2];\n"
						+ "old_data = old_row!==undefined?old_row[2]:data;\n"
						+ "hexChar = ['0', '1', '2', '3', '4', '5', '6', '7','8', '9', 'A', 'B', 'C', 'D', 'E', 'F'];\n");
			}
		});
		HorizontalPanel line = new HorizontalPanel();
		line.add(check);
		line.add(help);
		configRows.add(line);
		return text;
	}

	private native String evalBlock(String js, JsArrayMixed sqlRows,Compare[] csvCompareData, int currentRow)
	/*-{
			$wnd.sqlRows = sqlRows;
			$wnd.csvCompareData = csvCompareData;
			$wnd.currentRow = currentRow;
			var t = "function t(){"+
			"var sqlRows=$wnd.sqlRows;"+
			"var currentRow=$wnd.currentRow;"+
			"var csvCompareData=$wnd.csvCompareData;"+
			"var row=sqlRows[currentRow];"+
			"var old_row = currentRow>0?sqlRows[currentRow-1]:undefined;"+
			"var ts = row[1];"+
			"var old_ts = old_row!==undefined?old_row[1]:undefined;"+
			"var data = row[2];"+
			"var old_data = old_row!==undefined?old_row[2]:data;"+
			"var hexChar = ['0', '1', '2', '3', '4', '5', '6', '7','8', '9', 'A', 'B', 'C', 'D', 'E', 'F'];"+
			
			js+"}t();";
			return eval(t);
	}-*/;
	
	private void addCSVForm() {
		csvUploadPanel = new HorizontalPanel();
		final FileUploadExt uploadCSV = new FileUploadExt();
		uploadCSV.setName("file");
		csvUploadPanel.add(new Label("Select compare Data export:"));
		csvUploadPanel.add(uploadCSV);
		uploadCSV.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				FileList list = uploadCSV.getFiles();
				csvUploadPanel.setVisible(false);
				for(int i = 0; i < list.getLength();i++)
				{
					File file = list.getItem(i);
					final FileReader csvreader = new  FileReader();
					csvreader.addLoadEndHandler(new LoadEndHandler() {
						@Override
						public void onLoadEnd(LoadEndEvent event) {
							String csvdata = csvreader.getStringResult();
							csvFinishedLoading(csvdata);
						}
					});
					csvreader.readAsText(file);
				}
			}
		});
		header.add(csvUploadPanel);
	}
	
	private void addDataForm() {
		HorizontalPanel p = new HorizontalPanel();
		p.add(new Label("show "));
		count = new TextBox();
		count.setText("10");
		count.getElement().getStyle().setColor("black");
		p.add(count);
		p.add(new Label(" from "+sqlRows.length()+" records, starting@ "));
		
		start = new TextBox();
		start.setText("0");
		start.getElement().getStyle().setColor("black");
		p.add(start);
		Button show = new Button("show");
		show.getElement().getStyle().setColor("black");
		p.add(show);
		
		header.add(p);
		
		
		show.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				makeDataPanels();
				
			}
		});
	}
	private void makeDataPanels() {
		columns.clear();
		int s = Integer.parseInt(start.getText());
		int c = Integer.parseInt(count.getText());
		GWT.log("showing "+c+" records starting from "+s);
		final VerticalPanel[] cs = new VerticalPanel[c];
		for(int p = s; p < sqlRows.length() && p < s+c;p++)
		{
			cs[p-s] = new VerticalPanel();
			VerticalPanel vp = cs[p-s];
			
			vp.getElement().getStyle().setBorderColor("black");
			vp.getElement().getStyle().setBorderWidth(1, Unit.PX);
			vp.getElement().getStyle().setBorderStyle(BorderStyle.DASHED);
			vp.getElement().getStyle().setPadding(1, Unit.PX);
			vp.setWidth("220px");
			
			setPanel(sqlRows,vp,p,s);
			columns.add(vp);
		}
	}
	private void setPanel(JsArrayMixed rows,
			VerticalPanel vp, int p, int start) {
		
		for(int b = 0; b < blocks.length;b++)
		{
			HTML h = new HTML(evalBlock(blocks[b], rows, csvCompareData, p));
			vp.add(h);
		}
		/*
		table = new StringBuilder("<table width=\"100%\">");
		
		for(int i = 0; i < 48;i++)
		{
			table.append("<tr>");
			int rawV = ((rawblocks[i][1]<< 8)+rawblocks[i][0]);
			table.append("<td>"+(i<16?"Trend "+i:"History "+(i-16))+"</td>");
			table.append("<td>rawV: "+rawV+"</td>");
			table.append("<td>"+"/10: "+(((int)(rawV/10.0)*10)/10)+"</td>");
			//table.append("<td>"+"/6-36: "+(((int)(((rawV/6.0)-36)*10))/10)+"</td>");
			
//			GWT.log("ts="+ts);
			long soff = ts %60;
//			GWT.log("ts->60="+(ts-soff));
			long hoff = (ts-soff) % (15*60);
//			GWT.log("ts->15M="+((ts-soff)-hoff));
			
			if(i < 16)
			{
				int tm = i - index_trend + 1;
				if(tm>0)tm-=16;
				
				long t = (ts-soff)+(tm*60);
				
				GWT.log("indexT:"+t+" for i:"+i+" index_trend:"+index_trend+" tm:"+tm+" soff:"+soff+" ts:"+ts);
				for(Compare c : csvCompareData)
				{
					if(c.type==1 && Math.abs(c.ts-t) < 100)
					{
						GWT.log("Candidate(trend:"+(-tm)+"): "+c.gluc+" @ts: "+c.ts+" for t:"+t);
						table.append("<td>t:"+c.gluc+"</td>");
					}
				}
			}
			else
			{
				int tm = (i-16) - index_historie +1;
				if(tm > 0)
					tm -=32;
				long t = ((ts-soff)-hoff)-(tm*60*15);
				
				GWT.log("indexT:"+t+" for i:"+i+" index_hist:"+index_historie+" tm:"+tm+" soff:"+soff+" hoff:"+hoff+" ts:"+ts);
				for(Compare c : csvCompareData)
				{
					if(c.type==0 && Math.abs(c.ts-t) < 100)
					{
						GWT.log("Candidate(hist:"+tm+"): "+c.gluc+" @ts: "+c.ts+" for t:"+t);
						table.append("<td>h:"+c.gluc+"</td>");
					}
				}
			}
			table.append("</tr>");
		}
		table.append("</table>");
		vp.add(new HTML(table.toString()));
	
		if(p == start)
		{
			GWT.log("ts="+ts);
			long soff = ts %60;
			GWT.log("ts->60="+(ts-soff));
			long hoff = (ts-soff) % (15*60);
			GWT.log("ts->15M="+((ts-soff)-hoff));
			
			for(int i = 0; i < 16;i++)
			{
				long t = (ts-soff)-(i*60);
				for(Compare c : csvCompareData)
				{
					if(Math.abs(c.ts-t) < 100)
					{
						GWT.log("Candidate2(trend:"+i+"): "+c.gluc+" @ts: "+c.ts+" for t:"+t);
					}
				}
			}
			
			for(int i = 0; i < 32;i++)
			{
				long t = ((ts-soff)-hoff)-(i*60*15);
				for(Compare c : csvCompareData)
				{
					if(Math.abs(c.ts-t) < 100)
					{
						GWT.log("Candidate2(hist:"+i+"): "+c.gluc+" @ts: "+c.ts+" for t:"+t);
					}
				}
			}
		}
		*/
		
	}
}
