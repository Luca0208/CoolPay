package me.acul.coolpay;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


class FloatingScheduler implements Runnable {
    @Override
    public void run() {
        //Fixme: This seems to do not work, I currently don't have time to investigate further.
        if (!Coolpay.rootNode.getNode("players").isVirtual()) {
            //Check if there's any money going in
            Map<String, Map> addresses = (Map) Coolpay.rootNode.getNode("players").getValue();
            Iterator it = addresses.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String uuid = (String) pair.getKey();
                String address = Krist.makeV2Address(Coolpay.rootNode.getNode("players", uuid, "pass").getString());
                int balance = Krist.getBalance(address);
                if (balance > 0) {
                    String master = Krist.makeV2Address(Coolpay.rootNode.getNode("masterpass").getString());
                    System.out.println("Transmitted " + balance + " from " + address + " to " + master);
                    if (!Krist.transact(Coolpay.rootNode.getNode("players", uuid, "pass").getString(), master, balance)) {
                        Optional<Player> p = Sponge.getServer().getPlayer(UUID.fromString(uuid));
                        if (p.isPresent()) {
                            p.get().sendMessage(Text.builder("[CoolPay] Sorry! There has been a problem with your deposit, if this message keeps reappearing, please ask  an admin to help you!").color(TextColors.RED).build());
                        }

                    } else {
                        int old = Coolpay.rootNode.getNode("floating", uuid, "in").getInt();
                        Coolpay.rootNode.getNode("floating", uuid, "in").setValue(old + balance);
                    }

                }

                it.remove();
            }

        }

        Coolpay.saveConfig();
        if (!Coolpay.rootNode.getNode("floating").isVirtual()) {
            Map<String, Map> floating = (Map) Coolpay.rootNode.getNode("floating").getValue();
            for (Object o : floating.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                String uuid = (String) pair.getKey();
                int in = Coolpay.rootNode.getNode("floating", uuid, "in").getInt();
                if (in > 0) {

                    Optional<Player> p = Sponge.getServer().getPlayer(UUID.fromString(uuid));

                    if (in > 15000) {

                        int old = Coolpay.rootNode.getNode("players", uuid, "balance").getInt();
                        Coolpay.rootNode.getNode("players", uuid, "balance").setValue(old + 15000);
                        Coolpay.rootNode.getNode("floating", uuid, "in").setValue(in - 15000);
                        Coolpay.saveConfig();

                        if (p.isPresent()) {

                            p.get().sendMessage(Text.builder("[CoolPay] 15,000 KST have been transferred to your account, " + String.format("%,d KST", in - 15000) + " still floating.").color(TextColors.GREEN).build());

                        }

                    } else {

                        int old = Coolpay.rootNode.getNode("players", uuid, "balance").getInt();
                        Coolpay.rootNode.getNode("players", uuid, "balance").setValue(old + in);
                        Coolpay.rootNode.getNode("floating", uuid, "in").setValue(0);
                        Coolpay.saveConfig();

                        if (p.isPresent()) {

                            p.get().sendMessage(Text.builder("[CoolPay] " + String.format("%,d KST", in) + " have been transferred to your account. 0 KST still floating.").color(TextColors.GREEN).build());

                        }
                    }

                    Map<String, Integer> out = (Map) Coolpay.rootNode.getNode("floating", uuid, "out").getValue();

                    for (Object i : out.entrySet()) {

                        Map.Entry a = (Map.Entry) i;
                        String to = (String) a.getKey();
                        Integer amount = (Integer) a.getValue();

                        if (amount > 15000) {
                            if (Krist.transact(Coolpay.rootNode.getNode("masterpass").getString(), to, 15000)) {

                                Coolpay.rootNode.getNode("floating", uuid, "out", to).setValue(amount - 15000);
                                Coolpay.saveConfig();

                                if (p.isPresent()) {

                                    p.get().sendMessage(Text.builder("[CoolPay] 15,000 KST have been transferred to " + to + " " + String.format("%,d KST", amount - 15000) + " still floating.").color(TextColors.GREEN).build());

                                }

                            }
                        } else {

                            if (Krist.transact(Coolpay.rootNode.getNode("masterpass").getString(), to, amount)) {

                                Coolpay.rootNode.getNode("floating", uuid, "out", to).setValue(0);
                                Coolpay.saveConfig();

                                if (p.isPresent()) {

                                    p.get().sendMessage(Text.builder("[CoolPay] " + String.format("%,d KST", amount) + " have been transferred to " + to + " 0 KST still floating.").color(TextColors.GREEN).build());

                                }

                            } else {

                                if (p.isPresent()) {

                                    p.get().sendMessage(Text.builder("[CoolPay] There has been a problem with your transaction, if this message keeps reappearing please contact an admin!").color(TextColors.RED).build());

                                }
                            }

                        }
                    }
                }
            }
        }
    }
}