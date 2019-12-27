
var messageStack = [];

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
  
  var inhabitantsList = document.getElementById('inhabitantsList');
  var roomData = document.getElementById('roomData');
  var logData = document.getElementById('logData');
  var grabButton = document.getElementById('grabButton');

  var socket;

  let lastSentCommand;
  
  function output(style, text){
  //messages.innerHTML += "<br/><span class='" + style + "'>" + text + "</span>";
      if (!(text[0] === "{")) {
        messageStack.push("> "+style+" "+text);
        setUpLogData();
      }
  }

    // Open
    openBtn.onclick = function(e) {
        e.preventDefault();
        if (socket !== undefined) {
            output("error", "Already connected");
            return;
        }

        openBtn.style.background = "#4a4a4a";
        closeBtn.style.background = "#ffe6cc";
        
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
            
            let message = event.data;
            
            //output("received", "<<< " + message);
            updateUI(message);
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
    
//    function grabStuff(e) { dazna warkdazna wark dazna warkdazna warkdazna wark
//      if (socket == undefined) {
//        output("error", 'Not connected');
//        return;
//      }
//      
//      let name = grabButton.value;
//       socket.send("grab "+name);
//    };
//    
//    grabButton.onclick = grabStuff();
//    
    
    //works
    grabButton.onclick = function(e) {
      if (socket == undefined) {
        output('error', 'Not connected');
        return;
      }
      let name = grabButton.textContent;
      socket.send("grab "+name);
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
      
      // focus to console
      if (e.code === "Space") {
        input.focus();
      }
      
      // Moving
      if (e.code === "ArrowUp") {
        moveToGivenDirection("north");
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
      
      if (e.code === "ShiftRight") {
        if (socket != undefined) {
          let name = grabButton.textContent;
          socket.send("grab "+name);
        }
        else {
          output('error', 'Not connected');
        }
      }
    });
    
    function updateUI(data) {
      
      // Exclude " >" at the end !
      let transformedData = data.substring(0,data.length-3); 
      
      let currentState = parseJsonSafely(transformedData);
      
      // if not json
      if (currentState == null) {
        console.log("got null");
        // if not json, just a message, add directly to stack
        messageStack.push("> " + data);
        setUpLogData();
        return;
      }
      
      // else parcing and then push
      let messageToPush = "> U R now a the " + currentState.name + " room";
      messageStack.push(messageToPush);
      
      // UPDATING
      
      // set default state for nav btns
      let navigationButtons = [northButton, westButton, eastButton, southButton];
      for (i in navigationButtons)
        navigationButtons[i].style.color = "#4a4a4a";
        grabButton.style.visibility="hidden";
        
      // set the nav btns
      let exits = currentState.exits;
      
      for (index in exits) {
        switch (exits[index]) {
            case "north": northButton.style.color = "white"; break;
            case "west": westButton.style.color = "white"; break;
            case "east": eastButton.style.color = "white"; break;
            case "south": southButton.style.color = "white"; break;
        }
      }
      
      // Setting up inhabitants List
      let inhabitants = currentState.inhabitants;
      var inhabitantsString = "";
      
      for (i in inhabitants) {
        inhabitantsString += inhabitants[i].id + "  " + inhabitants[i].name + "<br>";
      }

      inhabitantsList.innerHTML = inhabitantsString;
      
      // Setting up room data
      var roomDataString = "";
      
      roomDataString += currentState.name + "<br><br>";
      roomDataString += currentState.desc + "<br>";
      
      roomDataString += "<br>Exits:<br>";     
      for (i in exits) {
        roomDataString += "- " + exits[i] + "<br>"
      }
      roomData.innerHTML = roomDataString;
      
      // Setting up LOG data
      setUpLogData();
      
      // Setting up grab button
      if (currentState.items.length>0) {
        let stuffName = currentState.items[0];
        
        grabButton.innerHTML = stuffName;
        grabButton.style.visibility = "visible";        
      }
    }
    
   function setUpLogData() {
    // Setting up LOG data
      var logDataString = "";
      
      for (var i = 7; i >= 1; i--) {
        if (messageStack.length-i >= 0){
          logDataString += messageStack[messageStack.length-i] + "<br>";
        }
      }
      
      logData.innerHTML = logDataString;

  }
    
};

function parseJsonSafely(json) {
  // game state 
  var parsedObject;
  try {
      parsedObject = JSON.parse(json);
      console.log(parsedObject.exits[0])
  } catch (e) {
    console.log('cant parse it');
  }
  return parsedObject;
}

