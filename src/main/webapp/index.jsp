<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta charset="utf-8" />
    <title>Chat</title>
    <meta name="description" content="chat">
    <meta name="author" content="Daneel Yaitskov">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="stylesheet" href="/static/bootstrap.min.css">
    <link rel="shortcut icon" href="/favicon.ico">
    <script type="text/javascript" src="/static/jquery.js"></script>
    <script type="text/javascript" src="/static/bootstrap.min.js"></script>
    <script type="text/javascript" src="/static/app.js"></script>
    <script type="text/javascript">
    </script>
</head>
<body>
<div class="container">
    <div class="accordion" id="root">
        <div class="accordion-group">
            <div class="accordion-heading">
                <a class="accordion-toggle pull-left" data-toggle="collapse" data-parent="#root"
                   href="#chat">
                    Chat
                </a>
                <a class="accordion-toggle" data-toggle="collapse" data-parent="#root"
                   href="#history">
                    History
                </a>
            </div>
            <div id="chat" class="accordion-body">
                <div class="accordion-inner">
                    chat
                </div>
            </div>
            <div id="history" class="accordion-body collapse">
                <div class="accordion-inner">
                    history
                </div>
            </div>
        </div>
    </div>
    <div class="clear"></div>
</div>
</body>
</html>
