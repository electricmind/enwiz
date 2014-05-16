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
    
    $(".my-panes").hide();
    $("#my-about").show();

    $(window).hashchange(function() {
        if (!$(location.hash).size() == 0) {
            $(".my-panes").hide();
            $($(location.hash)).show();
        }
    })

    $(window).hashchange();

})
