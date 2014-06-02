$(document).ready(function() {
    var interval;
    function progress() {
        $.ajax({
            url : "/json/progress",
            statusCode : {
                504 : function() {
                    $("#progress").text("Unavailable")
                }
            }
        }).done(function(response) {
            if (response.status.name == "OK") {
                $("#progress").replaceWith($("#progress-tmpl").tmpl({
                    tasks : response.data
                }));
                if (response.data.length == 0) {
                 clearInterval(interval);
                }
            } else {
                $("#progress").text("Unavailable")
            }
        });
    };
    
    var interval = setInterval(progress, 1000);
    
    $(".form-upload").ajaxForm({
        clearForm :true,
       success : function(responseText, statusText, xhr, $form)  {
           clearInterval(interval);
           interval = setInterval(progress, 1000);
       }
    });
    
});