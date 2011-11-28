 $(function() {
   $('.error').hide();
   $(".button").click(function() {
     // validate and process form here
    var query = $("input#query").val();
    var ranges = $("input#ranges").val();
    if(query == "") {
       return false;
    }
    if(ranges == "") {
       $("label#ranges_error").show();
       $("input#ranges").focus();
       return false;
    }

    var dataString = 'query='+ query + '&ranges=' + ranges;
    $('#message').html("<h2>Export in progress, please wait...</h2>");

    $.ajax({
      type: "POST",
      url: "/CrawlerSearchWebApp/export",
      data: dataString,
      success: function(data) {
        var result = JSON.parse(data);
        $('#message').html("<h2><a href=\""+result.url+"\">Download export file</a></h2>") ;
      }
    }); // end .ajax

   return false;
   }); //end button click
 });
