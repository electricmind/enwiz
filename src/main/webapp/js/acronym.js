$(document).ready(function() {
   $(".form-acronym .enwiz-loading").hide(100);
   const nmax = 2;
   var n = nmax;
   var allowed = true;
    $(".form-acronym").ajaxForm({
        //clearForm : true,
        beforeSubmit : function(status) {
                if (allowed) {
                    if (n == nmax) {
                        $(".form-acronym .enwiz-error").hide(100);
                        $(".form-acronym .enwiz-warning").hide(100);
                        $(".form-acronym .enwiz-submit").hide(0);
                        $(".form-acronym .enwiz-loading").show(0);
                    }
                    $(".form-acronym input[type=text]").attr("disabled","disabled");
                    allowed = false;
                    return true;
                } else {
                    return false;
                }
        },
        error : function(status) {
            $("#acronym .enwiz-error").show(100);
            $(".form-acronym .enwiz-submit").show(0);
            $(".form-acronym .enwiz-loading").hide(0);
            $(".form-acronym input[type=text]").removeAttr("disabled");
            allowed = true;
            n = nmax;
        },
        success : function(response, statusText, xhr, $form) {
            if (response.status.name == "OK" || response.status.name == "Best")  {
                $(".form-acronym .enwiz-submit").show(0);
                $(".form-acronym .enwiz-loading").hide(0);
                $(".form-acronym input[type=text]").removeAttr("disabled");
                allowed = true;
                n = nmax;
                $("#mnemonic-tmpl").tmpl({
                    mnemonic : response.data,
                    status : response.status.name
                }).appendTo($(".acronym")).show(200);
                $('.my-acronym-scroll').animate({
                    scrollTop : $('.my-acronym-scroll table').height()
                }, 200)
                $($form).resetForm();
            } else {
                    if (response.status.name == "Timeout" && n > 0) {
                        n--;
                        allowed = true;
                        $(".form-acronym input[type=text]").removeAttr("disabled");
                        $(".form-acronym").submit();
                    } else {
                        if (response.status.name == "Timeout") {
                            $("#acronym .enwiz-warning").show(100);
                        } else {
                            $("#acronym .enwiz-error").show(100);
                        }

                        $(".form-acronym .enwiz-submit").show(0);
                        $(".form-acronym .enwiz-loading").hide(0);
                        $(".form-acronym input[type=text]").removeAttr("disabled");
                        allowed = true;
                        n = nmax;
                    }
            }
        }
    });
});
