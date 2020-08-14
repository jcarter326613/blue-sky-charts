$(document).ready(() => {
    $(".submitForm").click((event) => {
        event.preventDefault();
        event.stopPropagation();

        let name = $("#name").val();
        let email = $("#email").val();
        let message = $("#message").val();
        name = encodeURIComponent(name.trim());
        email = encodeURIComponent(email.trim());
        message = message.trim();
        if ( name.length == 0 ) {
            alert("Name is a required field");
            return;
        }
        if ( email.length == 0 ) {
            alert("Email is a required field");
            return;
        }
        if ( message.length == 0 ) {
            alert("Message is a required field");
            return;
        }

        let e = "muddypawcloudtechnology";
        let ma = "@";
        let i = "gmail";
        let l = "com";
        let url = `https://api.blueskycharts.com/support/submitSupportRequest?name=${name}&email=${email}`;
        $.ajax({
            url: url,
            type: "POST",
            dataType: "text",
            crossDomain: true,
            cache: false,
            data: message,
            success: (data) => {
                alert("Your message has been received.  Someone will respond as soon as possible.  Thank you or contacting us.")
                window.location.href = "/";
            },
            error: (response) => {
                alert(`We could not send your message.  Please try again or send your message to ${e}${ma}${i}.${l}.`);
            }});
    })
});