// Prevent browser cache
$.ajaxSetup({cache: false});

// Max number of fields to display on Hover
$MAXFIELDS = 3;

// GFF Fields to display by default
$GFFFields = [ 'id' , 'chr' , 'feature', 'start', 'end' ];
$EMBLFields = [ ];

function testShowGffDoc() {
	data = '{"responseHeader":{"status":0,"QTime":30,"params":{"facet":"on","start":"0","q":"yeast","facet.field":"bank","qt":"shard","wt":"json"}},"response":{"numFound":16406,"start":0,"docs":[{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=FLO9","strand":".","id":"FLO9","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"ebdf15f9-c3d8-42ff-b959-d22b3f68ec1f","start":[24001],"end":[27969]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=CLN3;oneattr=sampleattr;anotherattr=anyvalue;test1=test;test2=newtest","strand":".","id":"CLN3","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"c1ddd825-5618-418b-bab8-69a8d802bdf9","start":[65779],"end":[67521]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=MAK16","strand":".","id":"MAK16","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"9e2cf512-8253-4cd7-a92f-d0b04f073fed","start":[100226],"end":[101146]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=CYS3","strand":".","id":"CYS3","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"5954d8ed-d19c-4815-a99e-8e9a4d40fd88","start":[130802],"end":[131986]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=ADE1","strand":".","id":"ADE1","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"7cba9dcd-2b2d-43be-853d-0339697af694","start":[169370],"end":[170290]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=PHO11","strand":".","id":"PHO11","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"166f32b1-85b4-49d4-a614-26f8b0fb0ba1","start":[225451],"end":[226854]},{"bank":"yeast","chr":"chrII","feature":"region","attributes":"ID=ILS1","strand":".","id":"ILS1","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"1d63704f-2c42-47cb-964c-e7650f7289f8","start":[81041],"end":[84259]},{"bank":"yeast","chr":"chrII","feature":"region","attributes":"ID=MCM2","strand":".","id":"MCM2","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"db31a7e0-eec4-4a18-9c19-864fb8e2ea96","start":[174923],"end":[177529]},{"bank":"yeast","chr":"chrII","feature":"region","attributes":"ID=RAD16","strand":".","id":"RAD16","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"19b55e74-f3d4-44c3-a177-385606a56987","start":[467242],"end":[469614]},{"bank":"yeast","chr":"chrII","feature":"region","attributes":"ID=SUP45","strand":".","id":"SUP45","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"3cd53a52-6120-4a29-b60c-269889532941","start":[530863],"end":[532176]}]},"facet_counts":{"facet_queries":{},"facet_fields":{"bank":["yeast",16406]},"facet_dates":{}}}';	
	var obj = jQuery.parseJSON(data);
  	size = obj.response.docs.length;
    for(s=0;s<size;s++) {
		doc = obj.response.docs.shift();
        var subclass = 'Odd';
        if(s%2==0) subclass= 'Even';
        showGffDoc(doc,subclass);
    }
}

 function showGffDoc(doc,subclass) {

	 content='<div id="gff_'+doc['id']+'" class="document'+subclass+'">';
	 content+='<div class="gff_main_fields">';
	 for(var field in $GFFFields) {
		 content+='<div class="field">'+$GFFFields[field]+': '+doc[$GFFFields[field]]+'</div>';
	 }
	 //content+='<div class="field">ID: '+doc['id']+'</div>';
	 //content+='<div class="field">CHR: '+doc['chr']+'</div>';
	 //content+='<div class="field">Position: '+doc['start']+'-'+doc['end']+'</div>';
	 content+='</div>';
	 
	 content+='<div class="doclinks">';
	 content+= detailsLink(doc);
	 content+='<a  href="'+$storageurl+'/dataquery.html?id='+doc['id']+'&source='+doc['chr']+'&start='+doc['start']+'&stop='+doc['end']+'" target="_blank"><img class="icon" title="Get source/transcript data" alt="Get raw data" src="images/iDocument.png"/></a>';
	 // GBrowse
	 if(doc['feature']!="est") {
	 content+='<a href="'+$gburl+'/gb2/gbrowse/'+$gbbank+'?name='+doc['chr']+':'+doc['start']+'..'+doc['end']+'" target="_blank"><img class="icon" alt="Browse genome" title="Browse genome" src="images/ibrowse.png"/></a>'   
	 }
	 content+='</div>';
	 
	 content+='</div>';
	
	 $("#documents").append(content);
	 
	 $("#gff_"+doc['id']).hover(
			  function () {
				  nbattr=0;
				  attr_content='<div id="gff_attr_'+doc['id']+'" class="gff_attr_fields">';
				//Split attributes
					var attr = doc['attributes'].split(";");
					for(var attrkey in attr) {
				   	 	var attribute = attr[attrkey].split("=");
				   	 	if(nbattr< $MAXFIELDS) {
				   	 	if(attribute[0].indexOf('ID')==-1 && attribute[0].indexOf('stream_')==-1 && attribute[0].indexOf('seqid')==-1 && attribute[0].indexOf('file')==-1) {
				   	 		attr_content+='<div class="attributesDetail">'+attribute[0]+': '+$.URLDecode(attribute[1])+'</div>';
				   	 		nbattr++;
				   	 	}
				   	 	}
				   	 	else {
				   	 		attr_content += '<div class="attributesDetail">...</div>';
				   	 		break;
				   	 	}
				   	 	
					}
				  attr_content+='</div>';
				  if(nbattr>0) {
			      $(this).append($(attr_content));
				  //$("#detailPanel").append(attr_content);
				  }
			  }, 
			  function () {
			    //$(this).find("gff_attr_fields").remove();
				$("#gff_attr_"+doc['id']).remove(); 
			  }
			);
	 
	//return content;
 }
 
 function showEmblDoc(doc,subclass) {
	 content='<div id="gff_'+doc['id']+'" class="document'+subclass+'">';
	 content+='<div class="embl_main_fields">';
	 for(var key in doc) {
			if(key.indexOf('stream_')==-1 && key.indexOf('seqid')==-1 && key.indexOf('id')==-1 && key.indexOf('file')==-1) {
				if(doc[key] instanceof Array) {
					content+='<div class="field">'+key+': ';
					content+=$.URLDecode(doc[key][0]);
					for(int arr=1;arr<doc[key].length;arr++) {
					content+=", "+$.URLDecode(doc[key][arr]);
					}
					content+='</div>';
				}
				else {
					content+='<div class="field">'+key+': '+$.URLDecode(doc[key])+'</div>';	
				}
			}
	  }
	 content+='</div>';
	 content+='<div class="doclinks">';
	 content+= detailsLink(doc);
	 content+='</div>';
	 content+='</div>';
	 $("#documents").append(content);
	 //return content;
	 
 }
 
 
 function showPdbDoc(doc,subclass) {
     content='<div id="pdb_'+doc['id']+'" class="document'+subclass+'">';
     content+='<div class="pdb_main_fields">';
     for(var key in doc) {
                    if(key.indexOf('stream_')==-1 && key.indexOf('seqid')==-1 && key.indexOf('file')==-1) {
                    content+='<div class="field">'+key+': '+$.URLDecode(doc[key])+'</div>';
                    }
      }
     content+='</div>';
     content+='<div class="doclinks">';
     content+= detailsLink(doc);
     content+='<a target="_blank" href="pdb.html?name='+$.URLEncode(doc["stream_name"])+'&file='+doc['file']+'"><img class="icon" title="View in 3D" alt="Show 3D structure" src="images/pdb.png"/></a>';
     content+='</div>';
     content+='</div>';
     $("#documents").append(content);

}
 
 function detailsLink(doc) {
	content='<img  onclick="showDetails(\''+doc["seqid"]+'\',\''+ doc["stream_content_type"] +'\',\''+ doc["stream_name"]  +'\',\''+ doc["file"]  +'\')" title="Show details" alt="Show details" src="images/ihelp.png" class="icon"/>';
	return content;
 }
 
 
 
 
 
 //  ------ Functions --------------------
 
 		function appendField(fieldname) {
 			$("input#query").val($("input#query").val()+" "+fieldname+":");
 		}
	   
	   function showFields() {
	   		$.get("fields.txt",
					   function(data){
					   var result = jQuery.parseJSON(data);
					   var fields = result["fields"];
					   var fieldlist = "";
					   for(var field in fields) {
					   	fieldlist += "<p onclick=\"appendField('"+fields[field]+"')\">"+fields[field]+"</p>";
					   }
					   $("#documentDetails" ).html(fieldlist);
					   $("#documentDetails" ).dialog({ title: 'List of available fields' , width : '300px' });
					   
			});
	   }
	   
	   function goToPage() {
			$("#documentDetails" ).html('<p class="wait"><img title="loading" alt="loading" src="images/waiting.gif"/></p>');
		 	$("#documentDetails" ).dialog({ title: 'Loading, please wait' , width : '200px'  });
	   		if($("#gotopage").val()!="") {
	   			doQuery($("input#query").val(),($("#gotopage").val() - 1) * $pageCountResults);
	   		}
	   }
	   
	   function showDetails(seqid,content_type,name,file) {
			
			// For others, open window to file or put in dialog? depends on size...
			$("#documentDetails" ).html('<p class="wait"><img title="loading" alt="loading" src="images/waiting.gif"/></p>');
		    $("#documentDetails" ).dialog({ title: 'Loading, please wait' , width : '200px' });
			if(content_type=="biosequence/gff") {
			  // Get gff doc from seqid
			  $.get($solrUrl+"q=seqid:"+seqid,
					   function(data){
					        $("#documentDetails" ).dialog( "close" );
					        
				   			var details="";
				   			var obj = jQuery.parseJSON(data);
				   			var doc = obj.response.docs[0];
				   			for(var key in doc) {
								if(key.indexOf('stream_')==-1 && key.indexOf('seqid')==-1 && key.indexOf('id')==-1 && key.indexOf('file')==-1) {
									if(key=="attributes") {
										//Split attributes
										var attr = doc[key].split(";");
										for(var attrkey in attr) {
											if(attrkey.indexOf('stream_')==-1 && attrkey.indexOf('seqid')==-1 && attrkey.indexOf('file')==-1) {									
									   	 	var attribute = attr[attrkey].split("=");
											details+='<div class="fieldDetail">'+attribute[0]+': '+$.URLDecode(attribute[1])+'</div>';
											}	
										}
									}
									else {
										details+='<div class="fieldDetail">'+key+': '+$.URLDecode(doc[key])+'</div>';		
									}
								}
							} // end for
							if(name) {
								// If a link to original file is present, add it with a link for download.
								   var fileurl = '<a target="_blank" href="lookup.jsp?file='+$.URLEncode(name)+'&content-type='+content_type;
									if(file) {
			  							fileurl += "&position="+file;
									}
									fileurl += '">Original sheet</a>';
									details+='<div class="fieldDetail">'+fileurl+'</div>';
							}
				   			$("#documentDetails" ).html(details);
							$("#documentDetails" ).dialog({ title: 'Document' , width : '600px'  });
					   },"application/json"
					 );
			}
			else {
			var fileurl = "lookup.jsp?file="+$.URLEncode(name)+"&content-type="+content_type;
			if(file) {
			  fileurl += "&position="+file;
			}
			window.open( fileurl );
	        $("#documentDetails" ).dialog( "close" );
			
			}
			
		};
	   
	   function submitFacet(key,facet) {
	   	$("input#query").val($("input#query").val()+" +"+key+":"+facet);
	    doQuery($("input#query").val(),0);
	   }
	   
	   function doQuery(solrQuery,start) {
	   		$pageCount=1;
	   		// Show loading windows
	   		$("#documentDetails" ).html('<p class="wait"><img title="loading" alt="loading" src="images/waiting.gif"/></p>');
		    $("#documentDetails" ).dialog({ title: 'Loading, please wait...' , width : '200px'  });
	   		
		    if($debugweb==1) {
		    		$("#documentDetails" ).dialog( "close" );
	   			  	data = '{"responseHeader":{"status":0,"QTime":30,"params":{"facet":"on","start":"0","q":"yeast","facet.field":"bank","qt":"shard","wt":"json"}},"response":{"numFound":1,"start":0,"docs":[{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=FLO9;test=testcontent;test2=testcontent2;test3=testcontent3;test4=testcontent4;test5=testcontent5","strand":".","id":"FLO9","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"ebdf15f9-c3d8-42ff-b959-d22b3f68ec1f","start":[24001],"end":[27969]}]}}';
		   			var obj = jQuery.parseJSON(data);
		   			$("#metadata").html("Found "+obj.response.numFound+" documents");
		   			if(obj.facet_counts) {
		   			 showFacets(obj.facet_counts.facet_fields);
		   			}
		   			if(obj.response.docs) {
		   			 showResults(obj.response.docs);
		   			 showNavigation(obj.response.numFound);
		   			}
		   			return;
		    }
		    
			$.get($solrUrl+$solrFacet+"q="+solrQuery+"&start="+start,
					   function(data){
					        $("#documentDetails" ).dialog( "close" );
				   			if($debugweb==1) {
				   			 data = '{"responseHeader":{"status":0,"QTime":30,"params":{"facet":"on","start":"0","q":"yeast","facet.field":"bank","qt":"shard","wt":"json"}},"response":{"numFound":16406,"start":0,"docs":[{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=FLO9","strand":".","id":"FLO9","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"ebdf15f9-c3d8-42ff-b959-d22b3f68ec1f","start":[24001],"end":[27969]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=CLN3","strand":".","id":"CLN3","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"c1ddd825-5618-418b-bab8-69a8d802bdf9","start":[65779],"end":[67521]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=MAK16","strand":".","id":"MAK16","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"9e2cf512-8253-4cd7-a92f-d0b04f073fed","start":[100226],"end":[101146]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=CYS3","strand":".","id":"CYS3","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"5954d8ed-d19c-4815-a99e-8e9a4d40fd88","start":[130802],"end":[131986]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=ADE1","strand":".","id":"ADE1","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"7cba9dcd-2b2d-43be-853d-0339697af694","start":[169370],"end":[170290]},{"bank":"yeast","chr":"chrI","feature":"region","attributes":"ID=PHO11","strand":".","id":"PHO11","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"166f32b1-85b4-49d4-a614-26f8b0fb0ba1","start":[225451],"end":[226854]},{"bank":"yeast","chr":"chrII","feature":"region","attributes":"ID=ILS1","strand":".","id":"ILS1","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"1d63704f-2c42-47cb-964c-e7650f7289f8","start":[81041],"end":[84259]},{"bank":"yeast","chr":"chrII","feature":"region","attributes":"ID=MCM2","strand":".","id":"MCM2","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"db31a7e0-eec4-4a18-9c19-864fb8e2ea96","start":[174923],"end":[177529]},{"bank":"yeast","chr":"chrII","feature":"region","attributes":"ID=RAD16","strand":".","id":"RAD16","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"19b55e74-f3d4-44c3-a177-385606a56987","start":[467242],"end":[469614]},{"bank":"yeast","chr":"chrII","feature":"region","attributes":"ID=SUP45","strand":".","id":"SUP45","stream_content_type":"biosequence/gff","stream_name":"/etc/gbrowse2/solrData/yeast/yeast.gff","seqid":"3cd53a52-6120-4a29-b60c-269889532941","start":[530863],"end":[532176]}]},"facet_counts":{"facet_queries":{},"facet_fields":{"bank":["yeast",16406]},"facet_dates":{}}}';
				   			}
				   			var obj = jQuery.parseJSON(data);
				   			$("#metadata").html("Found "+obj.response.numFound+" documents");
				   			if(obj.facet_counts) {
				   			 showFacets(obj.facet_counts.facet_fields);
				   			}
				   			if(obj.response.docs) {
				   			 showResults(obj.response.docs);
				   			 showNavigation(obj.response.numFound);
				   			}
				   			//Add facets,content and links for navigation
					   },"application/json"
					 );
	  }
		
	  function showNavigation(nbdocs) {
	  	
	  	if(nbdocs/$pageCountResults>10) {
	  		var nav="";
	  		for(var i=1;i<=5;i++) {
	  			nav += '<div class="nav" onclick="doQuery(\''+$("input#query").val()+'\','+((i-1)*$pageCountResults)+')">'+i+"</div>"; 	
	  		}
	  		nav += '<div class="nav">...';
	  		nav += '<input type="text" name="gotopage" id="gotopage" size="4" value="" class="text-input"/>';
	 		nav += '<button type="button" onclick="goToPage()">Go to page</button>';
	  		nav += '...</div>';
	  		
	  		for(var i=Math.ceil(nbdocs/$pageCountResults)-5;i<=Math.ceil(nbdocs/$pageCountResults);i++) {
	  			nav += '<div class="nav" onclick="doQuery(\''+$("input#query").val()+'\','+((i-1)*$pageCountResults)+')">'+i+"</div>"; 	
	 		}
	  		$("#navigation").html(nav); 	
	  	}
	  	else {
	  		var nav="";
	  		for(var i=1;i<=Math.ceil(nbdocs/$pageCountResults);i++) {
	  			nav += '<div class="nav" onclick="doQuery(\''+$("input#query").val()+'\','+((i-1)*$pageCountResults)+')">'+i+"</div>"; 	
	  		}
	  		$("#navigation").html(nav);
	  	}
	  
	  }
		
	  function showFacets(facets) {
	   var content = '<ul>';
	   for(var key in facets) {
	   		content += '<li class="facet">'+key+'</li><ul>';
	   		size = facets[key].length;
	   		for(s=0;s<size;s++) {
	   			facetlist = facets[key];
				facet = facetlist.shift();
				if(facet) {
					content+='<li class="facet" onclick="submitFacet(\''+key+'\',\''+facet+'\')">'+facet;
				}
				facet = facetlist.shift();
				if(facet) {
					content+=" ("+facet+")</li>\n";
				}			
			}
			content+="</ul>";	
		}	
		content+="</ul>";
		$("#facets").html(content);
	  }
	  
	  // Rewrite a field value if defined in config, replace #VAR# in config data with field value.
	  // Can be used to create some links to external data sources
	  // Ex.: rewrite genbank id to NCBI web site
	  function linkfield(bank,fieldname,fieldvalue) {
		 var value = fieldvalue;  
		 var rewrite = false;
		 var rewritevalue = fieldvalue;
		 if($links[bank+"."+fieldname]!=null) {
			 rewrite = true;
			 rewritevalue = $links[bank+"."+fieldname];
		 } else if ($links["all."+fieldname]) {
			 rewrite = true;
			 rewritevalue = $links["all."+fieldname];
		 }
		 if(rewrite) {
			 value = rewritevalue.replace(/#VAR#/g,fieldvalue);
		 }
 		 return value;
	  }
	  
	  function showResults(docs) {
		$("#documents").html("");
	  	size = docs.length;
	    for(s=0;s<size;s++) {
			doc = docs.shift();
	        var subclass = 'Odd';
	        if(s%2==0) subclass= 'Even';

				
			if(doc["stream_content_type"]=="biosequence/gff") { 
				showGffDoc(doc,subclass);
	        }
			else if(doc["stream_content_type"]=="biosequence/embl") {
				showEmblDoc(doc,subclass);
			}
			else if(doc["stream_content_type"]=="biosequence/pdb") {
				showPdbDoc(doc,subclass);
			}
			else {
				var content='<div class="document'+subclass+'">';
				for(var key in doc) {
					if(key.indexOf('stream_')==-1 && key.indexOf('seqid')==-1 && key.indexOf('file')==-1) {
					content+='<div class="field">'+key+': '+$.URLDecode(doc[key])+'</div>';		
					}
				}
				 content+='<div class="doclinks">';
				 content+= detailsLink(doc);
				 content+='</div>';
				content+='</div>\n';
				$("#documents").append(content);
			}
			
			
		}
		
	
	  
	  }	