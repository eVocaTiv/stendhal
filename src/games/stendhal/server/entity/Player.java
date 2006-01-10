/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.entity;

import games.stendhal.server.StendhalRPAction;
import games.stendhal.server.StendhalRPZone;
import games.stendhal.server.entity.creature.Sheep;
import games.stendhal.server.entity.item.Food;
import games.stendhal.server.entity.item.Item;
import games.stendhal.server.entity.item.Corpse;
import games.stendhal.server.entity.item.StackableItem;
import games.stendhal.server.entity.item.Money;
import games.stendhal.common.Rand;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import marauroa.common.Log4J;
import marauroa.common.game.AttributeNotFoundException;
import marauroa.common.game.RPClass;
import marauroa.common.game.RPObject;
import marauroa.common.game.RPSlot;

import org.apache.log4j.Logger;


public class Player extends RPEntity
  {
  /** the logger instance. */
  private static final Logger logger = Log4J.getLogger(Player.class);

  private List<Food> foodToEat;

  public static void generateRPClass()
    {
    try
      {
      RPClass player=new RPClass("player");
      player.isA("rpentity");
      player.add("text",RPClass.LONG_STRING, RPClass.VOLATILE);
      player.add("private_text",RPClass.LONG_STRING,(byte)(RPClass.PRIVATE|RPClass.VOLATILE));
      player.add("sheep",RPClass.INT);
      player.add("dead",RPClass.FLAG,RPClass.PRIVATE);

      player.add("outfit",RPClass.INT);
      
      player.add("reset",RPClass.FLAG,(byte)(RPClass.PRIVATE|RPClass.VOLATILE)); // The reset attribute is used to reset player position on next login

      // Use this for admin menus and usage.
      player.add("admin",RPClass.FLAG,RPClass.HIDDEN);
      player.add("invisible",RPClass.FLAG,RPClass.HIDDEN);

      // TODO: When we fix Marauroa myRPObject problem, changed slots to private.
      player.addRPSlot("head",1);
      player.addRPSlot("rhand",1);
      player.addRPSlot("lhand",1);
      player.addRPSlot("armor",1);
      player.addRPSlot("legs",1);
      player.addRPSlot("feet",1);
      player.addRPSlot("bag",12);
      player.addRPSlot("#flock",1,RPClass.HIDDEN);
      
      // We use this for the buddy system
      player.addRPSlot("!buddy",1,RPClass.HIDDEN);
      player.add("online",RPClass.LONG_STRING, (byte)(RPClass.PRIVATE|RPClass.VOLATILE));
      player.add("offline",RPClass.LONG_STRING, (byte)(RPClass.PRIVATE|RPClass.VOLATILE));
      
      player.addRPSlot("!quests",1,RPClass.HIDDEN);
      }
    catch(RPClass.SyntaxException e)
      {
      logger.error("cannot generateRPClass",e);
      }
    }
  
  public static Player create(RPObject object)
    {
    // Port from 0.03 to 0.10
    if(!object.has("base_hp"))
      {
      object.put("base_hp","100");
      object.put("hp","100");
      }

    // Port from 0.13 to 0.20
    if(!object.has("outfit"))
      {
      object.put("outfit",0);
      }
    
    // Port from 0.20 to 0.30
    if(!object.hasSlot("rhand"))
      {
      object.addSlot(new RPSlot("rhand"));
      }

    if(!object.hasSlot("lhand"))
      {
      object.addSlot(new RPSlot("lhand"));
      }

    if(!object.hasSlot("armor"))
      {
      object.addSlot(new RPSlot("armor"));
      }

    if(!object.hasSlot("head"))
      {
      object.addSlot(new RPSlot("head"));
      }

    if(!object.hasSlot("legs"))
      {
      object.addSlot(new RPSlot("legs"));
      }

    if(!object.hasSlot("feet"))
      {
      object.addSlot(new RPSlot("feet"));
      }

    if(!object.hasSlot("bag"))
      {
      object.addSlot(new RPSlot("bag"));
      }
    
    // Port from 0.30 to 0.35
    if(!object.hasSlot("!buddy"))
      {
      object.addSlot(new RPSlot("!buddy"));
      }
    
    if(!object.has("atk_xp"))
      {
      object.put("atk_xp","0");
      object.put("def_xp","0");
      }
    
    if(object.has("devel"))
      {
      object.remove("devel");
      }
    

    Player player=new Player(object);
    player.stop();
    player.stopAttack();
    
    boolean firstVisit=false;
    
    try
      {
      if(!object.has("zoneid")|| !object.has("x") || !object.has("y") || object.has("reset"))
        {
        firstVisit=true;
        }

      if(firstVisit)
        {
        player.put("zoneid","0_semos_city");        
        }

      world.add(player);
      }
    catch(Exception e) // If placing the player at its last position fails we reset it to city entry point
      {
      logger.warn("cannot place player at its last position. reseting to semos city entry point",e);
      
      firstVisit=true;
      player.put("zoneid","0_semos_city");        

      world.add(player);
      }

    StendhalRPAction.transferContent(player);

    StendhalRPZone zone=(StendhalRPZone)world.getRPZone(player.getID());
   
    if(firstVisit)
      {
      zone.placeObjectAtEntryPoint(player);
      }

    int x=player.getx();
    int y=player.gety();
        
    StendhalRPAction.placeat(zone,player,x,y);
    
    try
      {
      if(player.hasSheep())
        {
        logger.debug("Player has a sheep");
        Sheep sheep=player.retrieveSheep();
        sheep.put("zoneid",object.get("zoneid"));
        if(!sheep.has("base_hp"))
          {
          sheep.put("base_hp","10");
          sheep.put("hp","10");
          }

        world.add(sheep);
        x=sheep.getx();
        y=sheep.gety();
        StendhalRPAction.placeat(zone,sheep,x,y);
        player.setSheep(sheep);
        }
      }
    catch(Exception e) /** No idea how but some players get a sheep but they don't have it really.
                           Me thinks that it is a player that has been running for a while the game and 
                           was kicked of server because shutdown on a pre 1.00 version of Marauroa.
                           We shouldn't see this anymore. */
      {
      logger.error("Pre 1.00 Marauroa sheep bug. (player = "+player.getName()+")",e);

      if(player.has("sheep"))
        {
        player.remove("sheep");
        }
      
      if(player.hasSlot("#flock"))
        {
        player.removeSlot("#flock");
        }          
      }
    
    String[] slots={"rhand","lhand","head","armor","legs","feet","bag"};

    for(String slotName: slots)
      {
      try
        {
        if(player.hasSlot(slotName))
          {
          RPSlot slot=player.getSlot(slotName);
          
          List<RPObject> objects=new LinkedList<RPObject>();
          for(RPObject objectInSlot: slot)
            {
            objects.add(objectInSlot);
            }
          slot.clear();
          
          for(RPObject item: objects)
            {
            if(item.get("type").equals("item")) // We simply ignore corpses...
              {
              Item entity = world.getRuleManager().getEntityManager().getItem(item.get("name"));
              
              // HACK: We have to manually copy some attributes
              entity.put("#db_id",item.get("#db_id"));
              entity.setID(item.getID());
              
              if(entity instanceof StackableItem)
                {
                StackableItem money=(StackableItem)entity;
                money.setQuantity(item.getInt("quantity"));
                }
              
              slot.add(entity);
              }
            }
          }
        else
          {
          logger.warn("player "+player.getName()+" does not have the slot "+slotName);
          }
        }
      catch(Exception e)
        {
        logger.error("cannot create player",e);
        if (player.hasSlot(slotName))
          {
          RPSlot slot=player.getSlot(slotName);
          slot.clear();
          }
        }
      }
    
    if(player.getSlot("!buddy").size()>0)
      {
      RPObject buddies=player.getSlot("!buddy").iterator().next();
      for(String name: buddies)
        {
        if(name.charAt(0)=='_')
          {
          boolean online=false;
          for(Player buddy: rp.getPlayers())
            {
            if(name.equals(buddy.getName()))
              {
              player.notifyOnline(buddy.getName());
              online=true;
              break;            
              }          
            }
          
          if(!online)
            {
            player.notifyOffline(name.substring(1));
            }
          }
        }
      }

    if(!player.hasSlot("!quests"))
      {
      player.addSlot(new RPSlot("!quests"));
      RPSlot slot=player.getSlot("!quests");
      
      RPObject quests=new RPObject();
      slot.assignValidID(quests);
      slot.add(quests);
      }          

    player.setPrivateText("This release is EXPERIMENTAL. Please report problems, suggestions and bugs. You can find us at IRC irc.freenode.net #arianne");

    logger.debug("Finally player is :"+player);
    return player;
    }
    
  
  public static void destroy(Player player)
    {
    try
      {
      if(player.hasSheep())
        {
        Sheep sheep=(Sheep)world.remove(player.getSheep());
        player.storeSheep(sheep);
        rp.removeNPC(sheep);
        }
      else
        {
        // Bug on pre 0.20 released
        if(player.hasSlot("#flock"))
          {
          player.removeSlot("#flock");
          }
        }
      }
    catch(Exception e) /** No idea how but some players get a sheep but they don't have it really.
                           Me thinks that it is a player that has been running for a while the game and 
                           was kicked of server because shutdown on a pre 1.00 version of Marauroa.
                           We shouldn't see this anymore. */
      {
      logger.error("Pre 1.00 Marauroa sheep bug. (player = "+player.getName()+")",e);

      if(player.has("sheep"))
        {
        player.remove("sheep");
        }
      
      if(player.hasSlot("#flock"))
        {
        player.removeSlot("#flock");
        }          
      }

    player.stop();
    player.stopAttack();
    
    world.remove(player.getID());
    }

  public Player(RPObject object) throws AttributeNotFoundException
    {
    super(object);
    put("type","player");
    
    foodToEat=new LinkedList<Food>();  

    update();
    }

  public void update() throws AttributeNotFoundException
    {
    super.update();
    }

  public void setPrivateText(String text)
    {
    put("private_text", text);
    }

  public void getArea(Rectangle2D rect, double x, double y)
    {
    rect.setRect(x,y+1,1,1);
    }

  public void onDead(RPEntity who)
    {
    put("dead","");

    if(hasSheep())
      {
      // We make the sheep ownerless so someone can use it 
      Sheep sheep=(Sheep)world.get(getSheep());
      sheep.setOwner(null);
      
      remove("sheep");
      }

    super.onDead(who, false);

    // Penalize: Respawn on afterlive zone and 10% less experience
    subXP((int)(getXP()*0.1));
    
    setATK((int)(getATK()*0.9));
    setATKXP((int)(getATKXP()*0.9));
    
    setDEF((int)(getDEF()*0.9));
    setDEFXP((int)(getDEFXP()*0.9));
    
    setHP(getBaseHP());

    world.modify(who);

    StendhalRPAction.changeZone(this,"int_afterlife");
    StendhalRPAction.transferContent(this);
    }

  protected void dropItemsOn(Corpse corpse)
    {
    int amount=Rand.rand(4);
    
    if(amount==0)
      {
      return;      
      }
      
    String[] slots={"bag","rhand","lhand","head","armor","legs","feet"};

    for(String slotName: slots)
      {
      if(hasSlot(slotName))
        {
        RPSlot slot=getSlot(slotName);
        
        List<RPObject> objects=new LinkedList<RPObject>();
        for(RPObject objectInSlot: slot)
          {
          if(amount==0)
            {
            break;
            }

          if(Rand.roll1D6()<4)
            {
            objects.add(objectInSlot);
            amount--;
            }
          }
        
        for(RPObject object: objects)
          {
          if(object instanceof StackableItem)
            {
            StackableItem item=(StackableItem) object;

            double percentage=(Rand.rand(40)+10)/100.0;
            int quantity=item.getQuantity();

            item.setQuantity((int)(quantity*(1.0-percentage)));
            
            StackableItem rest_item=(StackableItem)world.getRuleManager().getEntityManager().getItem(object.get("name"));
            rest_item.setQuantity((int)(quantity*percentage));
            corpse.add(rest_item);
            }
          else if(object instanceof PassiveEntity)
            {
            slot.remove(object.getID());
          
            corpse.add((PassiveEntity)object);
            amount--;
            }            
          }
        }
      
      if(amount==0)
        {
        return;
        }
      }
    }

  public void removeSheep(Sheep sheep)
    {
    Log4J.startMethod(logger, "removeSheep");
    if(has("sheep"))
      {
      remove("sheep");
      }
    else
      {
      logger.warn("Called removeSheep but player has not sheep: "+this);      
      }

    rp.removeNPC(sheep);

    Log4J.finishMethod(logger, "removeSheep");
    }

  public boolean hasSheep()
    {
    return has("sheep");
    }

  public void setSheep(Sheep sheep)
    {
    Log4J.startMethod(logger, "setSheep");
    put("sheep",sheep.getID().getObjectID());

    rp.addNPC(sheep);

    Log4J.finishMethod(logger, "setSheep");
    }

  public static class NoSheepException extends RuntimeException
    {
    private static final long serialVersionUID = -6689072547778842040L;

    public NoSheepException()
      {
      super();
      }
    }

  public RPObject.ID getSheep() throws NoSheepException
    {
    return new RPObject.ID(getInt("sheep"),get("zoneid"));
    }

  public void storeSheep(Sheep sheep)
    {
    Log4J.startMethod(logger, "storeSheep");
    if(!hasSlot("#flock"))
      {
      addSlot(new RPSlot("#flock"));
      }

    RPSlot slot=getSlot("#flock");
    slot.clear();
    slot.add(sheep);
    put("sheep",sheep.getID().getObjectID());
    Log4J.finishMethod(logger, "storeSheep");
    }

  public Sheep retrieveSheep() throws NoSheepException
    {
    Log4J.startMethod(logger, "retrieveSheep");
    try
      {
      if(hasSlot("#flock"))
        {
        RPSlot slot=getSlot("#flock");
        if(slot.size()>0)
          {
          Iterator<RPObject> it=slot.iterator();

          Sheep sheep=new Sheep(it.next(),this);

          removeSlot("#flock");
          return sheep;
          }
        }

      throw new NoSheepException();
      }
    finally
      {
      Log4J.finishMethod(logger, "retrieveSheep");
      }
    }
  
  public void notifyOnline(String who)
    {    
    String playerOnline="_"+who;
    
    boolean found=false;
    RPSlot slot=getSlot("!buddy");
    if(slot.size()>0)
      {
      RPObject buddies=slot.iterator().next();
      for(String name: buddies)
        {
        if(playerOnline.equals(name))
          {
          buddies.put(playerOnline,1);
          world.modify(this);
          found=true;
          break;
          }
        }
      }
    
    if(found)
      {
      if(has("online"))
        {
        put("online",get("online")+","+who);
        }
      else
        {
        put("online",who);
        }
      }
    }
  
  public void notifyOffline(String who)
    {
    String playerOffline="_"+who;

    boolean found=false;
    RPSlot slot=getSlot("!buddy");
    if(slot.size()>0)
      {
      RPObject buddies=slot.iterator().next();
      for(String name: buddies)
        {
        if(playerOffline.equals(name))
          {
          buddies.put(playerOffline,0);
          world.modify(this);
          found=true;
          break;
          }
        }
      }
    
    if(found)
      {
      if(has("offline"))
        {
        put("offline",get("offline")+","+who);
        }
      else
        {
        put("offline",who);
        }
      }
    }
  
  public boolean isQuestCompleted(String name)
    {
    if(!hasSlot("!quests"))
      {
      logger.error("Expected to find !quests slot");
      return false;
      }
      
    RPSlot slot=getSlot("!quests");
    if(slot.size()==0)
      {
      logger.error("Expected to find something !quests slot");
      return false;
      }
    
    RPObject quests=(RPObject)slot.iterator().next();
    
    if(quests.has(name) && quests.getInt(name)==1)
      {
      return true;
      }
    else
      {
      return false;
      }
    }

  public boolean isQuestInProgress(String name)
    {
    if(!hasSlot("!quests"))
      {
      logger.error("Expected to find !quests slot");
      return false;
      }
      
    RPSlot slot=getSlot("!quests");
    if(slot.size()==0)
      {
      logger.error("Expected to find something !quests slot");
      return false;
      }
    
    RPObject quests=(RPObject)slot.iterator().next();
    
    if(quests.has(name) && quests.getInt(name)!=1)
      {
      return true;
      }
    else
      {
      return false;
      }
    }

  public void startQuest(String name)
    {
    if(!hasSlot("!quests"))
      {
      logger.error("Expected to find !quests slot");
      }
      
    RPSlot slot=getSlot("!quests");
    if(slot.size()==0)
      {
      logger.error("Expected to find something !quests slot");
      }
    
    RPObject quests=(RPObject)slot.iterator().next();
    quests.put(name,0);
    }

  public void completeQuest(String name)
    {
    if(!hasSlot("!quests"))
      {
      logger.error("Expected to find !quests slot");
      }
      
    RPSlot slot=getSlot("!quests");
    if(slot.size()==0)
      {
      logger.error("Expected to find something in !quests slot");
      }
    
    RPObject quests=(RPObject)slot.iterator().next();
    quests.put(name,1);
    }
  
  public void eat(Food food)
    {
    if(food.isContained())
      {
      // We modify the base container if the object change.
      RPObject base=food.getContainer();      

      while(base.isContained())
        {
        base=base.getContainer();
        }

      if(!nextto((Entity)base,0.25))
        {
        logger.debug("Food item is too far");
        return;
        }
      }
    else
      {
      if(!nextto(food,0.25))
        {
        logger.debug("Food item is too far");
        return;
        }
      }

    logger.debug("Consuming food: "+food.getAmount());    
    foodToEat.add(food);

    if(food.getQuantity()>1)
      {
      food.setQuantity(food.getQuantity()-1);
      if(!food.isContained())
        {
        world.modify(food);
        }
      }
    else
      {
      /* If quantity=1 then it means that item has to be removed */
      if(food.isContained())
        {
        // We modify the base container if the object change.
        RPObject base=food.getContainer();      
  
        while(base.isContained())
          {
          base=base.getContainer();
          }
        
        RPSlot slot=food.getContainerSlot();
        slot.remove(food.getID());
        
        world.modify(base);
        }
      else
        {
        world.remove(food.getID());
        }
      }
    }
  
  public void consume()
    {
    while(foodToEat.size()>0)
      {
      Food food=foodToEat.get(0);
      if(!food.consumed())
        {
        food.consume();
        int amount=food.getRegen();

        if(getHP()+amount<getBaseHP())
          {
          setHP(getHP()+amount);
          }
        else
          {
          setHP(getBaseHP());
          }

        world.modify(this);
        return;
        }
      else
        {
        foodToEat.remove(0);
        }
      }
    }
  }
