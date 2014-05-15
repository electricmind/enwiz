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
        }).done(function(data) {
            $("#progress").replaceWith($("#progress-tmpl").tmpl({
                tasks : data
            }));
                                            if (data.length == 0) {
             clearInterval(interval);
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