$(document).ready(
        function() {
            var cache = [];

            $(".remove").button().hide().on("click", function() {
                if ($(".phrase :last .word").size() == 2) {
                    if ($(".phrase").size() > 1) {
                        $(".phrase :last").remove();
                        $(".phrase :last .word :last").remove();
                    } else {
                        return;
                    }
                } else {
                    $(".phrase :last .word :last").remove();
                    if ($(".phrase :last .word").size() == 2 && $(".phrase").size() == 1) {
                        $(".remove").hide(200);
                    }
                }

                $(".words").remove();
                add();
            });

            function add() {
                var url = "/json/words/";
                var w2 = $(".phrase :last .word :last").data("word");
                var w1 = $(".phrase :last .word :last").prev().data("word");

                if (w1 != "") {
                    url += w1 + "/";
                }
                if (w2 != "") {
                    url += w2;
                }

                request(url);
            }

            function request(url, i) {

                i = typeof i !== 'undefined' ? i : 0;

                function update(data) {
                    $("#dropmenu-tmpl").tmpl({
                        wps : data
                    }).appendTo(".phrase :last");
                    $(".words").menu({
                        position : {
                            my : "top",
                            at : "right-5 top+5"
                        }
                    });
                    $(".menuitem").click(handler);
                }
                if (url in cache) {
                    update(cache[url]);
                } else {
                    $("#loading-tmpl").tmpl().appendTo(".phrase :last").button();

                    $.ajax(
                            {
                                url : url,
                                statusCode : {
                                    504 : function() {
                                        $(".loading").remove();
                                        if (i <= 1) {
                                            request(url, i + 1);
                                        } else {
                                            $("#reload-tmpl").tmpl({}).appendTo(
                                                    ".phrase :last").button();
                                            $(".reload").on("click",
                                                    function() {
                                                        $(".reload").remove();
                                                        request(url, 0);
                                                    })
                                        }
                                    }
                                }
                            }).done(function(data) {
                        cache[url] = data;
                        $(".loading").remove();
                        update(data);
                    });
                }
            }

            function handler(event) {

                var words = $(event.target).parents(".words");

                var w1 = $(words).prev().data("word");
                var w2 = $(event.target).data("word");

                $("#phraseitem-tmpl").tmpl({
                    "word" : $(event.target).data("word")
                }).appendTo(".phrase :last").show(400);
                $(".remove").show(400);


                $(words).remove();

                if (w2 == ".") {
                    $(".phrase :last").after($("#phrase-tmpl").tmpl());
                    add();
                } else {
                    add();
                }
                
                false;
            }
            add();
        })
