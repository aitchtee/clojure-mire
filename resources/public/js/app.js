window.onload = function() {
  var input = document.getElementById('input');
  var openBtn = document.getElementById('open');
//  var sendBtn = document.getElementById('send');
  var closeBtn = document.getElementById('close');
  var messages = document.getElementById('messages');
  
  var northButton = document.getElementById('north');
  var westButton = document.getElementById('west');
  var eastButton = document.getElementById('east');
  var southButton = document.getElementById('south');


  var socket;

  function output(style, text){
  messages.innerHTML += "<br/><span class='" + style + "'>" + text + "</span>";
  }

    // Open
    openBtn.onclick = function(e) {
        e.preventDefault();
        if (socket !== undefined) {
            output("error", "Already connected");
            return;
        }

        var uri = "ws://" + location.host + location.pathname;
        uri = uri.substring(0, uri.lastIndexOf('/'));
        socket = new WebSocket(uri);

        socket.onerror = function(error) {
            output("error", error);
        };

        socket.onopen = function(event) {
            output("opened", "Connected to " + event.currentTarget.url);
        };

        socket.onmessage = function(event) {
            var message = event.data;
            // messages.innerHTML = "<p> ulzfuk </br> ejlfsdkch</p>";
            output("received", "<<< " + message);
            
        };

        socket.onclose = function(event) {
            output("closed", "Disconnected: " + event.code + " " + event.reason);
            socket = undefined;
        };
    };

    // Send
//    sendBtn.onclick = function(e) {
//        if (socket == undefined) {
//            output("error", 'Not connected');
//            return;
//        }
//        var text = document.getElementById("input").value;
//        socket.send(text);
//        output("sent", ">>> " + text);
//    };    
    function sendCommand() {
      if (socket == undefined) {
        output("error", 'Not connected');
        return;
      }
      var text = document.getElementById("input").value;
      socket.send(text);
      input.value = '';
      output("sent", ">>> " + text);
    };
    
//    sendBtn.onclick = sendCommand();

    // Close
    closeBtn.onclick = function(e) {
        if (socket == undefined) {
            output('error', 'Not connected');
            return;
        }
        socket.close(1000, "Close button clicked");
    };
    
    // Movement Directions
    northButton.onclick = function (e) {
      moveToGivenDirection("north")
    };
    westButton.onclick = function (e) {
      moveToGivenDirection("west")
    };
    eastButton.onclick = function (e) {
      moveToGivenDirection("east")
    };
    southButton.onclick = function (e) {
      moveToGivenDirection("south")
    };
    
    function moveToGivenDirection(direction) {
      if (socket == undefined) {
        output('error', 'Not connected');
        return;
      }
      socket.send(direction);
      output("sent", ">>> " + direction);
    };
    
    document.addEventListener('keyup', (e) => {
      if (e.code === "Enter") {
        sendCommand();
      }
      
      if (e.code === "ArrowUp") {
        moveToGivenDirection("north")
      }
      if (e.code === "ArrowLeft") {
        moveToGivenDirection("west")
      }
      if (e.code === "ArrowRight") {
        moveToGivenDirection("east")
      }
      if (e.code === "ArrowDown") {
        moveToGivenDirection("south")
      }
      
    });
    
};

// Key Pressed Listeners
//window.onkeyup = function (e) {
//  let key = e.keyCode ? e.keyCode : e.which;
  

//  if (key == 13) {
//    alert(key);
//    sendCommand();
//  }
//};