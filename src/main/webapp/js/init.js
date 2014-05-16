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

    $(window).hashchange(function() {
//        alert(location.hash);
        
        $('#tabs').tabs('option', 'active', $('#tabs > div').index($(location.hash))-1);
    })

    $("#tabs").tabs();

    $(window).hashchange();

})
