<?php
header('Content-Type: text/html; charset=UTF-8');
header('Cache-Control: no-cache, must-revalidate');
header('Expires: Sat, 09 Jan 2009 09:09:09 GMT');
date_default_timezone_set('UTC');
if (!isset($_SERVER['HTTPS']) || $_SERVER['HTTPS'] !== 'on') die('https is required');

?><!DOCTYPE html><html lang="en"><head>
<meta charset="utf-8" />
<title>ilse</title>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=0.7">
<style>

html, body {
	color: #9c9;
	background: #333;
	font-family: sans-serif;
	min-width: 100%;
	min-height: 100%;
	padding: 0;
	margin: 0;
}
* {
	margin: 0;
	padding: 0;
	line-height: 1em;
	border: none;
	outline: none;
}
#fs_tab {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	width: 100%;
	height: 100%;
	z-index: 99;
}
#fs_tab,
#fs_tab tr,
#fs_tab td {
	border-collapse: collapse;
	background: inherit;
	color: inherit;
	font: inherit;
	text-align: center;
}
input {
	color: #cfc;
	background: #444;
	border: 1px solid #555;
	font: inherit;
}
#fs_tab input {
	font-size: 2em;
	padding: .3em .4em;
}
#fs_pwd {
	display: none;
}
#fs_msg {
	font-size: 2em;
}
#wrap {
	padding-top: 3em;
}
#head {
	position: fixed;
	font-size: 1em;
	top: 0;
	left: 0;
	right: 0;
	width: 100%;
	height: 1.5em;
}
#head_qb {
	position: fixed;
	top: .6em;
	left: .7em;
	right: 8.1em;
}
#q_btn {
	position: fixed;
	top: .7em;
	right: .7em;
	width: 6em;
}
#q {
	width: 100%;
}
#head input {
	padding: .2em .3em;
}
input:focus,
input:active {
	border-bottom: 1px solid #9c9;
}
#wrap>table td:nth-child(0),
#wrap>table td:nth-child(1),
#wrap>table td:nth-child(2),
#wrap>table td:nth-child(3) {
	white-space: nowrap;
}
#wrap td {
	padding: .2em .4em;
}
td[alt]:hover:after {
	content: attr(alt);
	margin: -9em;
	background: #333;
	padding: 0 2em;
}

</style></head><body>

<table id="fs_tab"><tr><td id="fs_td">
	<span id="fs_msg">
		Connecting
	</span>
	<span id="fs_pwd">
		<input type="password" id="pwd" name="pwd" />
		<input type="button" id="pwd_btn" value="Login" />
	</span>
</td></tr></table>

<div id="head">
	<input type="button" id="q_btn" value="Search" />
	<div id="head_qb">
		<input type="text" id="q" value="" />
	</div>
</div>

<div id="wrap">
	<h1>hello world</h1>
</div>

<script>

function o(n){return document.getElementById(n);}



o('pwd').addEventListener('keyup', function(ev)
{
	ev.preventDefault();
	if (ev.keyCode === 13)
		send_pwd();
});

o('pwd_btn').addEventListener('click', send_pwd, false);

function send_pwd()
{
	var pwd = o('pwd').value;
	ws.send(' ' + pwd + ' ');
}



var qto = null;

o('q').addEventListener('keyup', function(ev)
{
	ev.preventDefault();
	if (ev.keyCode === 13)
	{
		send_q();
	}
	else
	{
		window.clearTimeout(qto);
		qto = window.setTimeout(send_q, 50);
	}
});

o('q_btn').addEventListener('click', send_q, false);

function send_q()
{
	window.clearTimeout(qto);
	ws.send(o('q').value);
}



var ws = null;
if (window.location.href.substr(0,5) == 'https')
{
	ws = new WebSocket('wss://yoursite.yourTLD:9002/');
}
else
{
	alert('https is required');
}



var esc = function(unsafe) {
	if (!unsafe || unsafe == '')
		return '';

	return unsafe.replace(/[&<>]/g, function(m) {
		switch (m) {
			case '&':
				return '&amp;';
			case '<':
				return '&lt;';
			default:
				return '&gt;';
		}
	});
};



function msg(txt)
{
	o('fs_pwd').style.display = 'none';
	o('fs_msg').style.display = 'inline';
	o('fs_msg').innerHTML = txt;
	o('fs_tab').style.display = 'table';
	o('head').style.display = 'none';
	window.clearTimeout(qto);
}

function unmsg()
{
	o('fs_tab').style.display = 'none';
	o('head').style.display = 'block';
	o('q').focus();
}

ws.onerror = function()
{
	console.log('XXX socket error');
};

ws.onclose = function()
{
	console.log('XXX connection lost');
	o('fs_msg').style.fontVariant = 'small-caps';
	o('fs_msg').style.color = '#c90';
	msg("Connection lost");
};

ws.onmessage = function(e)
{
	var m = e.data;
	console.log('RX: %s', m);
	
	if (m.indexOf('server utc:') === 0)
	{
		o('fs_msg').style.display = 'none';
		o('fs_pwd').style.display = 'inline';
		o('pwd').focus();
	}

	if (m === '[AUTH_NG]')
	{
		o('pwd').value = '';
		o('pwd').focus();
		return;
	}

	if (m === '[AUTH_OK]')
	{
		o('fs_tab').style.display = 'none';
		o('q').focus();
		return;
	}

	if (m === '[REFRESH_START]')
	{
		msg("refreshing...");
		return;
	}
	
	if (m === '[REFRESH_DONE]')
	{
		o('q').value = '';
		unmsg();
		return;
	}

	if (m.substr(0, 11) === '{"hitcount"')
	{
		var jo = JSON.parse(m);
		html = ['<table><tr><td>net</td><td>chan</td><td>ts</td><td>from</td><td>' + jo.hitcount + '</td></tr>'];
		jo = jo.docs;
		//html.push('<tr><td><h1>' + jo.length + '</ht></td></tr>');
		for (var a = 0, aa = jo.length - 1; a < aa; a++)
		{
			var td = new Date(parseInt(jo[a].ts) * 1000);
			td = td.toISOString().replace('T',' ').substr(0, 19);

			html.push('<tr><td>' +
				esc(jo[a].net) + '</td><td>' +
				esc(jo[a].chan) + '</td><td alt="' +
				jo[a].ts + '">' +
				td + '</td><td>' +
				esc(jo[a].from) + '</td><td>' +
				esc(jo[a].msg) + '</td><tr>');
		}
		html.push('<tr><td>eof</td</tr></table>');
		o('wrap').innerHTML = html.join('\n');
	}
};
</script></body></html>

