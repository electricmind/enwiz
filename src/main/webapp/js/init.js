$(document).ready(function() {
    $(document).tooltip();
    $(".button").button();
    $(".menu").menu();
    $("#mainmenu").menu({
        position : {
            my : "top",
            at : "top+30"
        }
    });
    
    $("#tabs").tabs();
})
