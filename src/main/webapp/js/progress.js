$(document).ready(function() {
    var interval = setInterval(function() {
        $.ajax({
            url : "/json/progress",
            statusCode : {
                504 : function() {
                    $("#progress").text("Unavailable")
                }
            }
        }).done(function(data) {
            $("#progress").replaceWith($("#progress-tmpl").tmpl({
                tasks : data
            }));
            /*                                 if (data.length == 0) {
             //clearInterval(interval);
             }*/
        });
    }, 1000);

});
