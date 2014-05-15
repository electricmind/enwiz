$(document).ready(function() {
    $(document).tooltip();
    $(".button").button();
    $(".menu").menu();
    $("#mainmenu").menu();
    
    $(".my-panes").hide();
    $(".my-generate").show();

    $(".open-generate").click(function() {
        $(".my-panes").hide();
        $(".my-generate").show();
    });
    
    $(".open-upload").click(function() {
        $(".my-panes").hide();
        $(".my-upload").show();
    });
    
    $(".open-memento").click(function() {
        $(".my-panes").hide();
        $(".my-memento").show();
    });
    
    $(".open-about").click(function() {
        $(".my-panes").hide();
        $(".my-about").show();
    });
})
