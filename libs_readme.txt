It turned out that Android doesn't like .svn folders in 'libs' - it generates
currupted apk file in this case. So I've removed libs from the svn, and you
need to create 'libs/armeabi' and copy 'libtof.lib' from 'libs_' there before 
running.