- On a une méthode d'action dans le controller 
- Question de sécurité / protection

@Role("chef")
m1


@Authorized
m2 
- au niveau méthode 

comment : on declare dans le fichier de configuration le nom de la variable quand on est authentifié (web.xml ou fichier.properties) puis on donne ca à FrontServlet


Une nouvelle approche :
[authentifié]
m


[role = "prof"]
m2


role : tsy maintsy auth + role 

Login : login match login ; pwd match pwd => session.put("auth",...) ; session.put("role","admin")

UserSession : interface {
    String [] getRoles;
    boolean hasRole(String role);
}



Coté framework : on recherche la variable qui represente le role et auth dans la session créé par le dev via le fichier.properties ou web.xml 
soit on impose le nom de ces var

