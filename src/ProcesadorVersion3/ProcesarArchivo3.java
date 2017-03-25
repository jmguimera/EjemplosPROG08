package ProcesadorVersion3;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.*;


/*
 * Clase destinada a almacenar los datos de un artículo.
 */
class Articulo implements Comparable<Articulo>{
    public String codArticulo;
    public String descripcion;
    public int cantidad;

    @Override
    public int compareTo(Articulo o) {
        return codArticulo.compareTo(o.codArticulo);
    }

}


/* Clase que se encarga de procesar un pedido. 
 * El archivo con el pedido debe estar en codificación UTF8, sino no funciona
 * bien.
 * Tu sistema debe soportar de forma nativa la coficación UTF8, sino no 
 * funcionará del todo bien.
 */
public class ProcesarArchivo3 {

    /* Entrada contendrá una instancia de la clase Scanner que permitirá
    leer las teclas pulsadas desde teclado */
    static Scanner entrada = new Scanner(System.in);
    /* Definimos las expresiones regulares que usaremos una y otra vez para
     cada línea del pedido. La expresión regular "seccion" permite detectar
     si hay un comienzo o fin de pedido, y la expresión campo, permite detectar
     si hay un campo con información del pedido. */
    static Pattern seccion = Pattern.compile("^##[ ]*(FIN)?[ ]*(PEDIDO|ARTICULOS)[ ]*##$");
    static Pattern campo = Pattern.compile("^(.+):.*\\{(.*)\\}$");
    static Pattern articulo = Pattern.compile ("^\\{(.*)\\|(.*)\\|[ ]*([0-9]*)[ ]*\\}$");
    
    public static void main(String[] args) {
        BufferedReader lector;
        ArrayList<Articulo> Articulos=new ArrayList<Articulo>();
        HashMap<String,String> DatosPedido=new HashMap<String,String>();
        
        /* 
         * 1er paso: cargamos el archivo para poder procesarlo línea a línea
         * para ello nos apoyamos en la clase BufferedReader, que con el método
         * readLine nos permite recorrer todo el archivo línea a línea.
         */
        if (args.length > 0) {
            lector = cargarArchivo(args[0]);
        } else {
            lector = cargarArchivo();
        }


        if (lector == null) {
            /* Si no se ha podido cargar el archivo, no continúa con el 
             * procesado, simplemente termina la ejecución. */
            System.out.println("No se ha podido cargar el archivo.");
        } else {
            /* 2º Paso: si ha podido cargar el archivo, continúa el procesado ç
             * de línea a línea. */
            String linea;
            try {
                linea = lector.readLine();
                while (linea != null) {
                    procesarLinea(linea,DatosPedido,Articulos);
                    linea = lector.readLine();
                }
            } catch (IOException ex) {
                System.out.println("Error de entrada y salida.");
            }
            
            // 3er paso: Ordenamos los artículos por código.
            Collections.sort(Articulos);            
            
           
            /* 4er Paso: Pasamos el pedido a árbol DOM. */
            // Creamos un árbol DOM vacio
            Document doc=DOMUtil.crearDOMVacio("pedido");
            // Pasamos los datos del pedido al DOM
            pasarPedidoAXML(doc, DatosPedido, Articulos);
            // Guardamos el XML en un archivo:
            String salida;
            if (args.length>1) salida=args[1];
            else {
                System.out.print("Introduce el archivo de salida: ");
                salida=entrada.nextLine();
            }
            DOMUtil.DOM2XML(doc,salida);
        }
    }

    /**
     * Procesa una línea del archivo de pedido para detectar que es y 
     * extraer la información que contiene.
     * @param linea 
     * @param datosPedido Mapa en el que irá metiendo la información del pedido.
     * La llave del mapa será el nombre del campo.
     * @param articulos Lista en la que se irán metiendo los artículos del pedido.
     * @return true si la línea contiene información que corresponde al formato
     * esperado, false en caso contrario.
     */
    static boolean procesarLinea(String linea, Map<String,String> datosPedido,
            List<Articulo> articulos) {
           
        Matcher deteccionSeccion = seccion.matcher(linea);
        Matcher deteccionCampo = campo.matcher(linea);
        Matcher deteccionArticulo= articulo.matcher(linea); 
        /* Si el patrón coincide con el de un indicador de comienzo del pedido
         * o de la sección con el listado de artículos, se ejecutará este trozo
         * de código, pues habrá encontrado el patrón. No hace nada,
         * simplemente lo detecta para así no informar de algo raro.
         */
        if (deteccionSeccion.matches()) {
            return true;
        }
        /* Si el patrón coincide con el de un campo con datos del pedido
                 entonces meterá tanto el campo como el valor en el mapa.*/
        else if (deteccionCampo.matches()) {
            datosPedido.put(deteccionCampo.group(1).trim().toLowerCase(),
                    deteccionCampo.group(2).trim());
            return true;
        }
        /* Si el patrón coincide con el de un artículo, entonces 
           guardará los datos del pedido en una clase articulo y lo meterá
                 en la lista de artículos.*/
        else if (deteccionArticulo.matches())
        {
            Articulo n=new Articulo();
            n.codArticulo=deteccionArticulo.group(1).trim();
            n.descripcion=deteccionArticulo.group(2).trim();
            n.cantidad=Integer.parseInt(deteccionArticulo.group(3));
            articulos.add(n);
            return true;
        }
        else { System.out.println("¡Cuidado! Línea no procesable: "+linea); return false; }
    }

    /**
     * cargarArchivo creará una instancia de la clase BufferedReader que 
     * permitirá leer línea a línea el archivo de texto. Si no se ha podido 
     * cargar el archivo retornará null.
     * @param name Nombre del archivo a cargar. si el nombre del archivo no 
     * se ha pasado por parámetro (valor null) se pedirá al usuario que lo
     * introduzca.
     * @return null si no ha podido cargar el archivo, o la instancia de la 
     * clase BufferedReader si dicho archivo se ha podido cargar.
     */
    static BufferedReader cargarArchivo(String name) {
        String nombreArchivo = name;
        BufferedReader reader = null;
        if (name == null) {
            System.out.print("Introduce el nombre del archivo con el pedido: ");
            nombreArchivo = entrada.nextLine();
        }
        try {
            FileReader f =new FileReader(nombreArchivo);
            reader = new BufferedReader(f);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ProcesarArchivo3.class.getName()).log(Level.SEVERE, null, ex);
        }
        return reader;
    }

    /**
     * Igual que el método BufferedReader cargarArchivo(String name), pero
     * que siempre le pedirá al usuario que lo introduzca.
     * @return null si no ha podido cargar el archivo, y una instancia de BufferedReader
     * en otro caso.
     */
    static BufferedReader cargarArchivo() {
        return cargarArchivo(null);
    }
    
    /**
     * Función que pasa los datos del pedido, almacendos en un mapa y 
     * una lista, a un documento XML.
     * @param doc Documento DOM donde se almacenará toda la información.
     * @param datosPedido Mapa con los datos del pedido.
     * @param articulos Lista con los artículos del pedido.
     */
    static void pasarPedidoAXML(Document doc, 
            Map<String,String> datosPedido, 
            List<Articulo> articulos)
    {
        for (String key:datosPedido.keySet())
        {
            //Eliminamos espacios y acentos de cada llave para que no haya problemas de chequeo
            Element e=null;
            String key2=key.trim().replaceAll("\\s", "_").toLowerCase();
            key2=key2.replace("á", "a");
            key2=key2.replace("é", "e");
            key2=key2.replace("í", "i");
            key2=key2.replace("ó", "o");
            key2=key2.replace("ú", "u");
            if (key2.equals("nombre_del_contacto"))
                e=doc.createElement("contacto");
            else if (key2.equals("forma_de_pago"))
                e=doc.createElement("formaPago");
            else if (key2.equals("direccion_de_factura"))
                e=doc.createElement("dirFacturacion");
            else if (key2.equals("correo_electronico_del_contacto"))
                e=doc.createElement("mail");
            else if (key2.equals("codigo_del_cliente"))
                e=doc.createElement("codClient");
            else if (key2.equals("cliente"))
                e=doc.createElement("nombreCliente");
            else if (key2.equals("numero_de_pedido"))
                e=doc.createElement("numPedido");
            else if (key2.equals("telefono_del_contacto"))
                e=doc.createElement("telefono");
            else if (key2.equals("fecha_preferente_de_entrega"))
                e=doc.createElement("fechaPrefEntrega");
            else if (key2.equals("direccion_de_entrega"))
                e=doc.createElement("dirEntrega");
            
            if (e!=null) { 
                 e.setTextContent(datosPedido.get(key)); 
                 doc.getDocumentElement().appendChild(e);
            }
            else
            {
                Comment c=doc.createComment("Error procesando "+ key2);
                doc.getDocumentElement().appendChild(c);
            } 
        }
        Element arts=doc.createElement("listaArticulos");
        for (Articulo a:articulos)
        {
            Element art=doc.createElement("articulo");
            art.setAttribute("codArticulo", a.codArticulo);
            art.setAttribute("cantidad", a.cantidad+"");
            art.setTextContent(a.descripcion);
            arts.appendChild(art);
        }
        doc.getDocumentElement().appendChild(arts);
    }
}
