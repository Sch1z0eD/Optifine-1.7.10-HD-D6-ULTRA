package patch;

public interface HttpListener {

   void finished(HttpRequest var1, HttpResponse var2);

   void failed(HttpRequest var1, Exception var2);
}