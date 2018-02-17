#!/bin/bash
set -e
cd "$(dirname "${BASH_SOURCE[0]}")"

[[ "x$4" == "x" ]] &&
{
	echo
	echo "  need arg 1:  installation destination"
	echo "  need arg 2:  your webdomain"
	echo "  need arg 3:  internal websocket port (between java and nginx)"
	echo "  need arg 4:  external websocket port (between nginx and the world)"
	echo
	exit 1
}

dest="$1"
domain="$2"
wsint="$3"
wsext="$4"

echo
echo "  this installer will create the following files:"
echo "    $dest/index.php"
echo "    $dest/linkify.js"
echo

while true
do
	printf 'is that groovy [y/n] \033[1;33m'
	read -n1 -u1 -r
	printf '\033[0m\n'
	printf '%s\n' "$REPLY" | grep -qE '^[yY]$' && break
	printf '%s\n' "$REPLY" | grep -qE '^[nN]$' && exit 0
done

domain="$(printf '%s\n' "$domain" | sed -r 's/[\/&]/\\&/g')"


#
# end of user input
#
# begin actual installation
#


mkdir -p "$dest"

sed -r "
	s/yourdomain.yourTLD/$domain/g;
	s/([^0-9a-zA-Z])9002/\1$wsint/" < index.php > "$dest/index.php"

sed -r "
	s/yourdomain.yourTLD/$domain/g;
	s/([^0-9a-zA-Z])9002/\1$wsint/;
	s/([^0-9a-zA-Z])9001/\1$wsext/" < nginx.conf > "modified-nginx.conf"

{
	curl https://raw.githubusercontent.com/nfrasser/linkify-shim/master/linkify.min.js
	echo
	curl https://raw.githubusercontent.com/nfrasser/linkify-shim/master/linkify-string.min.js
} > "$dest/linkify.js"

echo
echo
echo "  almost done,"
echo "  you'll need to make the following nginx config changes:"
echo
echo "  $(pwd)/modified-nginx.conf"
echo
exit 0

