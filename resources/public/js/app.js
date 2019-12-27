window.onload = function() {
  var input = document.getElementById('input');
  var openBtn = document.getElementById('open');
  var sendBtn = document.getElementById('send');
  // var closeBtn = document.getElementById('close');
  var messages = document.getElementById('messages');
  var out = document.getElementById('out');
  var your_info = document.getElementById('your_info');
  
  var northButton = document.getElementById('north');
  var westButton = document.getElementById('west');
  var eastButton = document.getElementById('east');
  var southButton = document.getElementById('south');


  var socket;
  // var count_msg=0;

  function output(style, text){
  messages.innerHTML += "<br/><span class='" + style + "'>" + text + "</span>";
}

    // Open
    openBtn.onclick = function(e) {
        out.value = "";
        if (openBtn.innerHTML === "Start the game!") {
            e.preventDefault();
            // if (socket !== undefined) {
            //     output("error", "Already connected");
            //     return;
            // }

            var uri = "ws://" + location.host + location.pathname;
            uri = uri.substring(0, uri.lastIndexOf('/'));
            socket = new WebSocket(uri);

            socket.onerror = function(error) {
                // output("error", error);
                out.value += error;
            };

            socket.onopen = function(event) {
                // output("opened", "Connected to " + event.currentTarget.url);
                out.value += "\r\n" + "Connected to " + event.currentTarget.url;
            };

            socket.onmessage = function(event) {
                // count_msg++;
                // if (count_msg == 2) {out.value = ""; count_msg = 0;}
                var message = event.data;
                out.value += "\r\n" + message;
                if (message[0] === "{") {
                    var json = JSON.parse(message.substring(0,event.data.length-3));
                    your_info.innerHTML = "Id - " + json.your_id + "\t : \tName - " + json.your_name;
                }
                else{
                    // output("received", "<<< " + message);
                }
            };

            socket.onclose = function(event) {
                // output("closed", "Disconnected: " + event.code + " " + event.reason);
                out.value += "\r\n" + "Disconnected: " + event.code + " " + event.reason;
                socket = undefined;
            };
            openBtn.innerHTML = "Give up!";
        }
        else{
            // if (socket == undefined) {
            //     output('error', 'Not connected');
            //     return;
            // }
            socket.close(1000, "Close button clicked");
            openBtn.innerHTML = "Start the game!";
        }
    };

    // Send
    sendBtn.onclick = function(e) {
        if (socket == undefined) {
            // output("error", 'Not connected');
            out.value += 'Not connected';
            return;
        }
        var text = document.getElementById("input").value;
        document.getElementById("input").value = "";
        socket.send(text);
        // output("sent", ">>> " + text);
        out.value += text;
    };

    // Close
    // closeBtn.onclick = function(e) {
    //     if (socket == undefined) {
    //         output('error', 'Not connected');
    //         return;
    //     }
    //     socket.close(1000, "Close button clicked");
    // };
    
    // Movement Directions
    northButton.onclick = function (e) {
      moveToGivenDirection("north")
    }
    westButton.onclick = function (e) {
      moveToGivenDirection("west")
    }
    eastButton.onclick = function (e) {
      moveToGivenDirection("east")
    }
    southButton.onclick = function (e) {
      moveToGivenDirection("north")
    }
    
    function moveToGivenDirection(direction) {
      if (socket == undefined) {
        // output('error', 'Not connected');
        out.value += 'Not connected';
        return;
      }
      socket.send(direction);
      // output("sent", ">>> " + direction);
      out.value += direction;
    }
};