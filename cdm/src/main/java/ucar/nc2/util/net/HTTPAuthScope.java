/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.net;


import org.apache.http.auth.AuthScope;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The standard AuthScope does not provide sufficiently
 * fine grain authorization. So this class extends it
 * to support principals and datasets.
 */

@org.apache.http.annotation.Immutable
public class HTTPAuthScope extends AuthScope implements Cloneable
{

    //////////////////////////////////////////////////
    // Constants

    public static final String ANY_PRINCIPAL = null;
    public static final String ANY_PATH = null;

    public static final HTTPAuthScope ANY
            = new HTTPAuthScope(ANY_HOST, ANY_PORT, ANY_SCHEME, ANY_PRINCIPAL, ANY_PATH);

    //////////////////////////////////////////////////
    // Instance Variables

    protected String principal;
    protected String path;

    // Get around the fact that the fields of AuthScope are private
    protected String authscheme;
    protected String realm;
    protected String host;
    protected int port;

    //////////////////////////////////////////////////
    // Constructor(s)

    public HTTPAuthScope(String host, int port, String scheme, String principal, String path)
    {
        super(host, port, ANY_REALM, scheme); // set but ignore
        this.authscheme = scheme;
        this.realm = realm;
        this.host = host;
        this.realm = null;
        this.principal = principal;
        this.path = path;
    }

    public HTTPAuthScope(String host, int port, String realm, String scheme)
    {
        this(host, port, scheme, ANY_PRINCIPAL, ANY_PATH);
    }


    public HTTPAuthScope(AuthScope scope, String principal, String path)
    {
        this(scope.getHost(), scope.getPort(), scope.getScheme(), principal, path);
    }

    public HTTPAuthScope(AuthScope scope)
    {
        this(scope, ANY_PRINCIPAL, ANY_PATH);
    }

    public HTTPAuthScope(String surl)
            throws HTTPException
    {
        this(surl, ANY_SCHEME);
    }

    public HTTPAuthScope(String surl, String authscheme)
            throws HTTPException
    {
        super(ANY_HOST, ANY_PORT, ANY_REALM, ANY_SCHEME); // set but ignore
        URI uri = decompose(surl);
        this.authscheme = authscheme;
        this.host = uri.getHost();
        this.port = uri.getPort();
        this.principal = uri.getUserInfo();
        this.path = uri.getPath();
    }

    //////////////////////////////////////////////////	
    // URL Decomposition

    static URI decompose(String suri)
            throws HTTPException
    {
        try {
            URI uri = new URI(suri);
            return uri;
        } catch (URISyntaxException use) {
            throw new HTTPException("HTTPAuthScope: illegal url: " + suri);
        }
    }

    //////////////////////////////////////////////////	
    // Accessors	

    @Override
    public String getScheme()
    {
        return this.authscheme;
    }

    protected String getAuthScheme() // better named alias
    {
        return getScheme();
    }

    @Override
    public String getHost()
    {
        return this.host;
    }

    @Override
    public int getPort()
    {
        return this.port;
    }

    public String getPrincipal()
    {
        return this.principal;
    }

    public String getPath()
    {
        return this.path;
    }

    //////////////////////////////////////////////////
    // Misc. APIs

    public boolean valid()
    {
        return (getAuthScheme() != null && getHost() != null);
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s%s%s%s",
                getAuthScheme(),
                (getPrincipal() == null ? "" : getPrincipal() + "@"),
                getHost(),
                (getPort() < 0 ? "" : ":" + getPort()),
                getPath());
    }


    //////////////////////////////////////////////////
    // (De-)Serialization

    private void writeObject(java.io.ObjectOutputStream oos)
            throws IOException
    {
        oos.writeObject(getAuthScheme());
        oos.writeObject(getHost());
        oos.writeInt(getPort());
        oos.writeObject(getAuthScheme());
        oos.writeObject(getRealm());
        oos.writeObject(getPrincipal());
        oos.writeObject(getPath());
    }

    private void readObject(java.io.ObjectInputStream ois)
            throws IOException, ClassNotFoundException
    {
        this.authscheme = (String) ois.readObject();
        this.host = (String) ois.readObject();
        this.port = ois.readInt();
        this.realm = (String) ois.readObject();
        this.principal = (String) ois.readObject();
        this.path = (String) ois.readObject();
    }


    //////////////////////////////////////////////////
    // Equals and Equivalence interface


    /**
     * Equivalence algorithm:
     * if any field is ANY_XXX, then they are equivalent.
     * Scheme, port, host must all be identical else return false
     * If this.path is prefix of other.path
     * or other.path is prefix of this.path
     * or they are string equals, then return true
     * else return false.
     */
    static boolean equivalent(HTTPAuthScope a1, HTTPAuthScope a2)
    {
        if (a1 == null || a2 == null)
            throw new NullPointerException();
        if (a1.getScheme() != ANY_SCHEME && a2.getScheme() != ANY_SCHEME
                && !a1.getScheme().equals(a2.getScheme()))
            return false;
        if (a1.getHost() != ANY_HOST && a2.getHost() != ANY_HOST
                && !a1.getHost().equals(a2.getHost()))
            return false;
        if (a1.getPort() != ANY_PORT && a2.getPort() != ANY_PORT
                && a1.getPort() != a2.getPort())
            return false;
        if (a1.getPath() == ANY_PATH || a2.getPath() == ANY_PATH)
            return true;
        if (a1.getPath().startsWith(a2.getPath())
                || a2.getPath().startsWith(a1.getPath()))
            return true;
        return false;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof HTTPAuthScope))
            return false;
        HTTPAuthScope has = (HTTPAuthScope) other;
        return (equivalent(this, (HTTPAuthScope) other)
                && this.getPath().equals(has.getPath()));
    }

    /**
     * Check is an HTTPAuthScope is "subsumed" by an AuthScope.
     * Rules are a subset of equivalence.
     */
    static boolean subsumes(AuthScope as, HTTPAuthScope has)
    {
        if (as == null || has == null)
            throw new NullPointerException();
        if (as.getScheme() != ANY_SCHEME && has.getScheme() != ANY_SCHEME
                && !as.getScheme().equals(has.getScheme()))
            return false;
        if (as.getHost() != ANY_HOST && has.getHost() != ANY_HOST
                && !as.getHost().equals(has.getHost()))
            return false;
        if (as.getPort() != ANY_PORT && has.getPort() != ANY_PORT
                && as.getPort() != has.getPort())
            return false;
        return true;
    }

    //////////////////////////////////////////////////
    // Cloneable interface

    public Object clone()
    {
        return new HTTPAuthScope(this, this.principal, this.path);
    }

}
